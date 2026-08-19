package com.vpnclient.domain

/**
 * A back-channel: WireguardVpnService (lives in :data) reports the actual
 * connection status back to the repository, instead of the UI optimistically
 * assuming "connected" right after starting the service. Kept separate from
 * TunnelRepository because it has a different consumer (the service, not
 * the UI) — the two contracts shouldn't be mixed.
 */
interface TunnelStatusReporter {
    fun reportConnecting()
    fun reportConnected()
    fun reportDisconnected()
    fun reportFailed(reason: String)
}
