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
`src/bin/test_server.rs` и `VpnConfig` в `data/.../DefaultTunnelRepository.kt` —
они должны совпадать). С Android-эмулятора сервер на хост-машине доступен по
адресу `10.0.2.2`.

## Архитектура: многомодульность

Проект разбит на независимые Gradle-модули с чёткими границами зависимостей —
так же, как это делают в командах, где несколько разработчиков работают над
разными фичами параллельно, не мешая друг другу:

```
VpnClient/
├── domain/            чистый Kotlin, БЕЗ Android SDK. Модели, интерфейсы
│                       (TunnelRepository, TunnelStatusReporter), ничего лишнего.
├── data/               реализация domain-интерфейсов: WireguardVpnService,
│                       DefaultTunnelRepository, UniFFI-биндинги, .so из rust/.
├── feature-selftest/   экран self-test (Compose + ViewModel). Знает только :domain.
├── feature-connect/    экран подключения к серверу. Тоже знает только :domain.
├── app/                тонкий модуль: Koin DI, NavHost с deep links, манифест.
├── rust/vpn-core/       Rust-крейт с криптографией (WireGuard через boringtun).
└── jniLibs/             скомпилированные .so под Android (arm64-v8a, x86_64).
```

Направление зависимостей: `feature-*` и `data` зависят от `domain`, но никогда
наоборот. `feature-*` никогда не зависят от `data` напрямую — конкретную
реализацию (`DefaultTunnelRepository`) связывает Koin в `app/VpnClientApp.kt`.
Это значит, что `feature-selftest` тестируется без Android и без Rust — достаточно
подставить fake-реализацию `TunnelRepository`.

`WireguardVpnService` не может напрямую вызывать UI-код — вместо этого он
сообщает статус через `TunnelStatusReporter` (тот же объект, что и
`TunnelRepository`, просто другой интерфейс), а UI узнаёт об изменениях через
`StateFlow`, не зная о существовании сервиса.

**Presentation-слой — строгий MVI**, не MVVM с прямыми вызовами методов:
UI отправляет `Intent` (`viewModel.onIntent(SelfTestIntent.RunSelfTest)`,
`viewModel.onIntent(ConnectIntent.Connect)`), не дёргает методы ViewModel
напрямую. Состояние каждого экрана — один объект (`SelfTestUiState`,
`ConnectionState`), а не разрозненные поля. Исключение — системный диалог
`VpnService.prepare()`: платформенный шаг согласия остаётся в Composable
(нужен Activity-контекст), реальный intent пользователя уходит в ViewModel
только после подтверждения.

**Deep links:** `vpnclient://selftest` и `vpnclient://connect` открывают
конкретный экран напрямую, минуя навигацию внутри приложения — так фичи
открывают друг друга по ссылке, не зная внутреннего устройства.

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
