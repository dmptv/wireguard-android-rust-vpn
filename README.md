# VpnClient

Учебный Android VPN-клиент на протоколе WireGuard. Криптографическое ядро — на Rust
([BoringTun](https://github.com/cloudflare/boringtun), реализация WireGuard от
Cloudflare), подключено к Kotlin через [UniFFI](https://mozilla.github.io/uniffi-rs/)
(автогенерация биндингов, без ручного JNI).

## Что здесь показано

- Rust-крейт (`rust/vpn-core`) с реальной криптографией x25519 и полноценным
  WireGuard-handshake (не заглушка — используется настоящая библиотека протокола).
- UniFFI-мост: Rust `struct`/`enum`/объекты автоматически становятся Kotlin
  `data class`/`sealed class`/классами с `close()`.
- Кросс-компиляция под Android (`arm64-v8a`, `x86_64`) через `cargo-ndk`.
- Настоящий `android.net.VpnService`: поднимает TUN-интерфейс, шифрует исходящий
  трафик и расшифровывает входящий через Rust-ядро.
- `protect()` на UDP-сокете — без него VPN замкнул бы сам себя в бесконечную петлю.

## Как запустить (без какой-либо настройки)

1. Открыть в Android Studio, дождаться Gradle sync.
2. Запустить на эмуляторе или устройстве.
3. Нажать **«Self-test (офлайн, без сервера)»**.

Приложение создаёт два независимых объекта `WireguardTunnel` («клиент» и «сервер»)
прямо внутри себя и проводит между ними настоящий криптографический handshake —
шаг за шагом, с реальными байтами каждого пакета. Это доказывает, что весь пайплайн
Rust → UniFFI → Kotlin работает, без необходимости поднимать какую-либо
инфраструктуру.

## Подключение к настоящему серверу

Кнопка **«Подключиться к серверу»** запускает полноценный `VpnService` с TUN-интерфейсом
и требует реальный WireGuard-эндпоинт. В репозитории есть минимальный тестовый сервер:

```bash
cd rust/vpn-core
cargo run --bin test_server -- 51999
```

Он слушает указанный порт с захардкоженной парой ключей (см.
`src/bin/test_server.rs` и `VpnConfig` в `MainActivity.kt` — они должны совпадать).
С Android-эмулятора сервер на хост-машине доступен по адресу `10.0.2.2`.

## Структура проекта

```
VpnClient/
├── app/                        Android-приложение (Kotlin, Jetpack Compose)
│   └── .../WireguardVpnService.kt   настоящий VpnService: TUN + UDP + Rust-ядро
├── rust/vpn-core/               Rust-крейт с криптографией
│   ├── src/lib.rs                публичный API: generate_keypair, WireguardTunnel
│   └── src/bin/
│       ├── test_server.rs        тестовый WireGuard-сервер
│       └── test_client.rs        тестовый клиент (для локальной проверки без Android)
└── jniLibs/                     скомпилированные .so под Android (arm64-v8a, x86_64)
```

## Тесты Rust-ядра

```bash
cd rust/vpn-core
cargo test
```

Среди тестов — полный handshake между двумя туннелями в памяти, и отдельно —
handshake через настоящие UDP-сокеты на loopback (доказывает, что API годится
для реального сетевого ввода-вывода, а не только для передачи байт в памяти).

## Технический стек

Kotlin, Jetpack Compose, Rust, [boringtun](https://github.com/cloudflare/boringtun),
[UniFFI](https://github.com/mozilla/uniffi-rs), `cargo-ndk`, `android.net.VpnService`.
