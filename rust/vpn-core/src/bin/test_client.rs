// One-shot test client, used to verify that test_server responds correctly
// over a real network connection before driving the same flow from the
// Android emulator.
//
// Usage: cargo run --bin test_client -- <port> <server_pub_key_b64> <client_priv_key_b64>

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
    println!("sent handshake init, {} bytes", init.len());

    let n = socket.recv(&mut buf).expect("recv failed");
    println!("received response from server, {n} bytes");
    tunnel.decapsulate(buf[..n].to_vec());

    println!("handshake complete, is_expired = {}", tunnel.is_expired());

    let encrypted = match tunnel.encapsulate(b"hello from real network test".to_vec()) {
        TunnAction::SendToNetwork { data } => data,
        other => panic!("unexpected: {other:?}"),
    };
    socket.send(&encrypted).expect("send data failed");
    println!("sent encrypted message, {} bytes", encrypted.len());
}
