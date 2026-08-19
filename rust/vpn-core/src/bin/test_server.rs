// Minimal standalone WireGuard responder, used to verify that a client
// establishes a handshake with a real separate process over real UDP,
// not just with an object living in the same process memory.
//
// Usage: cargo run --bin test_server -- 51820
// Prints the server's public key — enter it in the client app as its
// "peer public key".

use std::env;
use std::net::UdpSocket;
use vpn_core::{TunnAction, WireguardTunnel};

// Fixed keys, generated once and hardcoded on both sides (here and in the
// client app) — the same way a real VPN provider registers a client's key
// on its server once, at account setup. A private key checked into a
// public repository is not something you would do in production; it's
// fine for a local, reproducible demo.
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
    println!("Test WireGuard server listening on port {port} with fixed keys");
    println!("Waiting for a connection...");

    let mut buf = [0u8; 2048];
    loop {
        let (n, from) = socket.recv_from(&mut buf).expect("recv failed");
        match tunnel.decapsulate(buf[..n].to_vec()) {
            TunnAction::SendToNetwork { data } => {
                socket.send_to(&data, from).ok();
                println!("[handshake] replied to {from}");
            }
            TunnAction::WriteToTunnel { data } => {
                println!(
                    "[data] received a decrypted packet from {from}, {} bytes: {:?}",
                    data.len(),
                    String::from_utf8_lossy(&data)
                );
            }
            TunnAction::Nothing => {}
        }
    }
}
