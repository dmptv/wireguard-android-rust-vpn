package com.vpnclient.domain

/**
 * The concrete errors [Result] carries in this app. Grounded in what can
 * actually fail today: WireguardVpnService's UDP link to the peer (Network)
 * and the on-device tunnel/self-test pipeline (Local) — see
 * DefaultTunnelRepository and WireguardVpnService in :data.
 */
sealed interface DataError : Error {

    /** Failures reaching or talking to the VPN peer over the network. */
    enum class Network : DataError {
        REQUEST_TIMEOUT,
        NO_CONNECTION,
        SERVER_UNREACHABLE,
        SERIALIZATION,
        UNKNOWN,
    }

    /** Failures that happen entirely on-device, with no network involved. */
    enum class Local : DataError {
        TUN_INTERFACE_UNAVAILABLE,
        HANDSHAKE_FAILED,
        UNKNOWN,
    }
}
