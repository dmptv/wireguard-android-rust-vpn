package com.vpnclient.domain

import kotlinx.coroutines.flow.Flow

/**
 * Contract for working with the VPN tunnel. No feature module sees
 * WireguardTunnel, UniFFI, or the Android VpnService directly — only this
 * interface. The implementation (:data) is wired in through DI (Koin,
 * configured in :app), so it can be swapped for a fake in tests without
 * touching any feature module.
 */
interface TunnelRepository {

    /**
     * Runs a full handshake between two in-process tunnels, with no network
     * involved, to verify the cryptographic pipeline. Returns the whole
     * list of steps at once (the computation itself is instant; staggering
     * the reveal is the presentation layer's concern).
     */
    suspend fun runSelfTest(): List<SelfTestStep>

    /** Observable connection state to the server. */
    fun connectionState(): Flow<ConnectionState>

    /** Starts connecting to the given server (async; observe connectionState() for progress). */
    fun connect(server: Server)

    fun disconnect()
}
