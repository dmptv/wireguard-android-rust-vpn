package com.vpnclient.domain

/**
 * Connection state to a real WireGuard server.
 * `sealed` so feature-connect must handle every case in a `when` — enforced
 * by the compiler.
 */
sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data object Connected : ConnectionState
    data class Failed(val reason: String) : ConnectionState
}
