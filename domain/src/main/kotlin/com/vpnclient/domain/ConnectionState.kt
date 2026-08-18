package com.vpnclient.domain

/**
 * Состояние подключения к настоящему WireGuard-серверу.
 * sealed — feature-connect обязан обработать все случаи в `when`, компилятор проверит.
 */
sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data object Connected : ConnectionState
    data class Failed(val reason: String) : ConnectionState
}
