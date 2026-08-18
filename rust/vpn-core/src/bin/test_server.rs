// Минимальный "живой" WireGuard-сервер для проверки, что Android-клиент реально
// устанавливает handshake с настоящим процессом по настоящему UDP, а не только
// с объектом в той же памяти.
//
// Запуск:  cargo run --bin test_server -- 51820
// Печатает публичный ключ сервера — его надо вставить в приложение как "peer public key".

use std::env;
use std::net::UdpSocket;
use vpn_core::{TunnAction, WireguardTunnel};

// Фиксированные ключи — сгенерированы один раз и захардкожены с обеих сторон
// (тут и в MainActivity.kt), точно так же, как настоящий VPN-провайдер один раз
// прописывает ключ клиента на своём сервере при регистрации аккаунта.
// В реальном проде так делать нельзя (приватный ключ сервера не должен лежать в git),
// но для локальной демонстрации — нормально и предсказуемо.
const SERVER_PRIVATE_KEY: &str = "+U89ptK1MxzEk4Y0v+Ig0CM3FgvShytJiSJzHsRamTk=";
const CLIENT_PUBLIC_KEY: &str = "6IUSlHr4Y2NWXlE6kFJxW9i2mwQCrK36kydYTdBnLDA=";

fn main() {
    let port: u16 = env::args()
        .nth(1)
        .and_then(|s| s.parse().ok())
        .unwrap_or(51820);

    let tunnel = WireguardTunnel::new(SERVER_PRIVATE_KEY.to_string(), CLIENT_PUBLIC_KEY.to_string())
        .expect("valid keys");

    let socket = UdpSocket::bind(("0.0.0.0", port)).expect("failed to bind UDP socket");
    println!("Тестовый WireGuard-сервер слушает порт {port}, ключи фиксированные");
    println!("Ожидаю подключение...");

    let mut buf = [0u8; 2048];
    loop {
        let (n, from) = socket.recv_from(&mut buf).expect("recv failed");
        match tunnel.decapsulate(buf[..n].to_vec()) {
            TunnAction::SendToNetwork { data } => {
                socket.send_to(&data, from).ok();
                println!("[handshake] ответил клиенту {from}");
            }
            TunnAction::WriteToTunnel { data } => {
                println!(
                    "[data] получен расшифрованный пакет от {from}, {} байт: {:?}",
                    data.len(),
                    String::from_utf8_lossy(&data)
                );
            }
            TunnAction::Nothing => {}
        }
    }
}
