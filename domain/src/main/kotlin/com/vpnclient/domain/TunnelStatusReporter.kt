package com.vpnclient.domain

/**
 * Обратный канал: WireguardVpnService (живёт в :data) сообщает репозиторию о
 * реальном статусе, а не притворяется, что "подключено" сразу после старта сервиса.
 * Отдельный от TunnelRepository интерфейс, потому что у него другой потребитель
 * (сервис, а не UI) — не стоит смешивать в одном контракте.
 */
interface TunnelStatusReporter {
    fun reportConnecting()
    fun reportConnected()
    fun reportDisconnected()
    fun reportFailed(reason: String)
}
