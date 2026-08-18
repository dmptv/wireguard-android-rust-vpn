package com.vpnclient.domain

import kotlinx.coroutines.flow.Flow

/**
 * Контракт работы с VPN-туннелем. Ни одна feature-модуль не видит WireguardTunnel,
 * UniFFI или Android VpnService напрямую — только этот интерфейс. Реализация (:data)
 * подставляется через DI (Koin, настраивается в :app), поэтому data можно подменить
 * на fake-реализацию в тестах, не трогая feature-модули.
 */
interface TunnelRepository {

    /**
     * Офлайн-демонстрация: полный handshake между двумя туннелями внутри процесса,
     * без сети. Возвращает список шагов целиком (сами вычисления мгновенны;
     * анимация появления — забота presentation-слоя).
     */
    suspend fun runSelfTest(): List<SelfTestStep>

    /** Наблюдаемое состояние подключения к настоящему серверу. */
    fun connectionState(): Flow<ConnectionState>

    /** Запускает подключение к серверу (асинхронно, состояние наблюдается через connectionState()). */
    fun connect()

    fun disconnect()
}
