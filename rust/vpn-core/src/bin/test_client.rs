// Одноразовый тестовый клиент — только чтобы САМОМУ проверить, что test_server реально
// отвечает по-настоящему через сеть, прежде чем гонять то же самое через Android-эмулятор.
//
// Запуск: cargo run --bin test_client -- <port> <server_pub_key_b64> <client_priv_key_b64>

use std::env;
use std::net::UdpSocket;
use vpn_core::{TunnAction, WireguardTunnel};

fn main() {
    let args: Vec<String> = env::args().collect();
    let port = &args[1];
    let server_pub = args[2].clone();
    let client_priv = args[3].clone();

    let tunnel = WireguardTunnel::new(client_priv, server_pub).expect("valid keys");

    let socket = UdpSocket::bind("127.0.0.1:0").expect("bind failed");
    socket
        .connect(("127.0.0.1", port.parse::<u16>().unwrap()))
        .expect("connect failed");

    let mut buf = [0u8; 2048];

    let init = match tunnel.encapsulate(vec![]) {
        TunnAction::SendToNetwork { data } => data,
        other => panic!("unexpected: {other:?}"),
    };
    socket.send(&init).expect("send failed");
    println!("отправил handshake init, {} байт", init.len());

    let n = socket.recv(&mut buf).expect("recv failed");
    println!("получил ответ от сервера, {n} байт");
    tunnel.decapsulate(buf[..n].to_vec());

    println!("handshake завершён, is_expired = {}", tunnel.is_expired());

    let encrypted = match tunnel.encapsulate(b"hello from real network test".to_vec()) {
        TunnAction::SendToNetwork { data } => data,
        other => panic!("unexpected: {other:?}"),
    };
    socket.send(&encrypted).expect("send data failed");
    println!("отправил зашифрованное сообщение, {} байт", encrypted.len());
}
