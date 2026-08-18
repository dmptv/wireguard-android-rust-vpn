use base64::{engine::general_purpose::STANDARD, Engine};
use boringtun::noise::{Tunn, TunnResult};
use boringtun::x25519::{PublicKey, StaticSecret};
use std::sync::{Arc, Mutex};

uniffi::setup_scaffolding!();

/// Буфер под один WireGuard-пакет. С запасом для MTU 1500 + заголовки протокола.
const MAX_PACKET: usize = 2048;

#[derive(uniffi::Record)]
pub struct KeyPair {
    pub private_key_base64: String,
    pub public_key_base64: String,
}

#[uniffi::export]
pub fn generate_keypair() -> KeyPair {
    let private = StaticSecret::random_from_rng(rand::rngs::OsRng);
    let public = PublicKey::from(&private);

    KeyPair {
        private_key_base64: STANDARD.encode(private.to_bytes()),
        public_key_base64: STANDARD.encode(public.as_bytes()),
    }
}

#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum TunnelError {
    #[error("key is not valid base64")]
    InvalidKeyEncoding,
    #[error("key must be exactly 32 bytes")]
    InvalidKeyLength,
}

fn decode_key(base64_key: &str) -> Result<[u8; 32], TunnelError> {
    let bytes = STANDARD
        .decode(base64_key)
        .map_err(|_| TunnelError::InvalidKeyEncoding)?;
    bytes.try_into().map_err(|_| TunnelError::InvalidKeyLength)
}

/// Что делать с результатом encapsulate/decapsulate/tick — на Kotlin-стороне
/// это станет sealed-подобным enum, по которому удобно сделать `when`.
#[derive(uniffi::Enum, Debug, PartialEq, Eq)]
pub enum TunnAction {
    /// Отправить эти байты по UDP на сервер.
    SendToNetwork { data: Vec<u8> },
    /// Записать эти (уже расшифрованные) байты в TUN-интерфейс — это IP-пакет для приложений.
    WriteToTunnel { data: Vec<u8> },
    /// Ничего делать не нужно (например, дубликат keepalive или ошибка на низком уровне).
    Nothing,
}

fn to_action(result: TunnResult<'_>) -> TunnAction {
    match result {
        TunnResult::WriteToNetwork(data) => TunnAction::SendToNetwork {
            data: data.to_vec(),
        },
        TunnResult::WriteToTunnelV4(data, _) | TunnResult::WriteToTunnelV6(data, _) => {
            TunnAction::WriteToTunnel {
                data: data.to_vec(),
            }
        }
        TunnResult::Done | TunnResult::Err(_) => TunnAction::Nothing,
    }
}

/// Обёртка над `boringtun::noise::Tunn` — сам туннель WireGuard.
/// Мьютекс внутри нужен, потому что Tunn не потокобезопасен сам по себе
/// (у него изменяемое состояние: сессии, таймеры) — так же делает и сам boringtun в своём JNI.
#[derive(uniffi::Object)]
pub struct WireguardTunnel {
    inner: Mutex<Tunn>,
}

#[uniffi::export]
impl WireguardTunnel {
    /// Создаёт туннель из своего приватного ключа и публичного ключа собеседника (пира).
    #[uniffi::constructor]
    pub fn new(
        private_key_base64: String,
        peer_public_key_base64: String,
    ) -> Result<Arc<Self>, TunnelError> {
        let private_bytes = decode_key(&private_key_base64)?;
        let peer_public_bytes = decode_key(&peer_public_key_base64)?;

        let private = StaticSecret::from(private_bytes);
        let peer_public = PublicKey::from(peer_public_bytes);

        let tunn = Tunn::new(private, peer_public, None, None, 0, None);

        Ok(Arc::new(Self {
            inner: Mutex::new(tunn),
        }))
    }

    /// Проверка, что туннель ещё жив (сессия не протухла).
    pub fn is_expired(&self) -> bool {
        self.inner.lock().unwrap().is_expired()
    }

    /// Принимает сырой IP-пакет из TUN-интерфейса (или пустой срез — тогда это
    /// способ запустить/повторить handshake) и решает, что с ним делать дальше.
    pub fn encapsulate(&self, packet: Vec<u8>) -> TunnAction {
        let mut dst = [0u8; MAX_PACKET];
        let result = self.inner.lock().unwrap().encapsulate(&packet, &mut dst);
        to_action(result)
    }

    /// Принимает UDP-датаграмму, пришедшую от сервера, и решает, что с ней делать:
    /// записать расшифрованные данные в TUN или отправить ответный пакет по сети
    /// (это происходит во время handshake).
    pub fn decapsulate(&self, datagram: Vec<u8>) -> TunnAction {
        let mut dst = [0u8; MAX_PACKET];
        let result = self
            .inner
            .lock()
            .unwrap()
            .decapsulate(None, &datagram, &mut dst);
        to_action(result)
    }

    /// Периодический вызов (обычно раз в секунду) — держит handshake живым,
    /// шлёт keepalive-пакеты, повторяет handshake, если сервер не ответил вовремя.
    pub fn tick(&self) -> TunnAction {
        let mut dst = [0u8; MAX_PACKET];
        let result = self.inner.lock().unwrap().update_timers(&mut dst);
        to_action(result)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::net::UdpSocket;

    #[test]
    fn generates_a_valid_looking_keypair() {
        let pair = generate_keypair();
        assert_eq!(pair.private_key_base64.len(), 44);
        assert_eq!(pair.public_key_base64.len(), 44);
    }

    #[test]
    fn constructs_a_tunnel_between_two_real_keypairs() {
        let client = generate_keypair();
        let server = generate_keypair();

        let tunnel = WireguardTunnel::new(client.private_key_base64, server.public_key_base64)
            .expect("valid keys should construct a tunnel");

        assert!(!tunnel.is_expired());
    }

    #[test]
    fn rejects_a_malformed_key() {
        let result = WireguardTunnel::new("not-base64!!".to_string(), "also-bad".to_string());
        assert!(result.is_err());
    }

    /// Полный реальный handshake в памяти, теперь через публичный API с TunnAction.
    #[test]
    fn completes_a_real_handshake_and_encrypts_data() {
        let client_keys = generate_keypair();
        let server_keys = generate_keypair();

        let client = WireguardTunnel::new(
            client_keys.private_key_base64,
            server_keys.public_key_base64.clone(),
        )
        .unwrap();
        let server = WireguardTunnel::new(
            server_keys.private_key_base64,
            client_keys.public_key_base64,
        )
        .unwrap();

        let init = match client.encapsulate(vec![]) {
            TunnAction::SendToNetwork { data } => data,
            other => panic!("ожидали SendToNetwork с handshake init, получили {other:?}"),
        };

        let response = match server.decapsulate(init) {
            TunnAction::SendToNetwork { data } => data,
            other => panic!("ожидали SendToNetwork с handshake response, получили {other:?}"),
        };

        client.decapsulate(response);

        let encrypted = match client.encapsulate(b"hello wireguard".to_vec()) {
            TunnAction::SendToNetwork { data } => data,
            other => panic!("ожидали зашифрованные данные, получили {other:?}"),
        };
        assert!(encrypted.len() > "hello wireguard".len());
    }

    /// То же самое, но handshake-пакеты реально идут через настоящие UDP-сокеты
    /// на loopback — доказывает, что дизайн API годится для настоящего сетевого ввода-вывода,
    /// а не только для передачи байт напрямую в памяти.
    #[test]
    fn handshake_completes_over_real_udp_sockets() {
        let client_keys = generate_keypair();
        let server_keys = generate_keypair();

        let client_tunnel = WireguardTunnel::new(
            client_keys.private_key_base64,
            server_keys.public_key_base64.clone(),
        )
        .unwrap();
        let server_tunnel = WireguardTunnel::new(
            server_keys.private_key_base64,
            client_keys.public_key_base64,
        )
        .unwrap();

        let server_socket = UdpSocket::bind("127.0.0.1:0").unwrap();
        let server_addr = server_socket.local_addr().unwrap();
        let client_socket = UdpSocket::bind("127.0.0.1:0").unwrap();
        client_socket.connect(server_addr).unwrap();

        // 1. Клиент генерит handshake init и реально отправляет его по UDP.
        let init = match client_tunnel.encapsulate(vec![]) {
            TunnAction::SendToNetwork { data } => data,
            other => panic!("expected init, got {other:?}"),
        };
        client_socket.send(&init).unwrap();

        // 2. Сервер реально принимает пакет из сокета.
        let mut buf = [0u8; MAX_PACKET];
        let (n, from) = server_socket.recv_from(&mut buf).unwrap();
        assert_eq!(from, client_socket.local_addr().unwrap());

        let response = match server_tunnel.decapsulate(buf[..n].to_vec()) {
            TunnAction::SendToNetwork { data } => data,
            other => panic!("expected response, got {other:?}"),
        };
        server_socket.send_to(&response, from).unwrap();

        // 3. Клиент реально принимает ответ обратно из сокета.
        let n = client_socket.recv(&mut buf).unwrap();
        client_tunnel.decapsulate(buf[..n].to_vec());

        assert!(!client_tunnel.is_expired());
        assert!(!server_tunnel.is_expired());
    }
}
