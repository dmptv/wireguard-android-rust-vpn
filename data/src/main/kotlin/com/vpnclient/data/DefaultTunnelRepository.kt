package com.vpnclient.data

import android.content.Context
import android.content.Intent
import com.vpnclient.domain.ConnectionState
import com.vpnclient.domain.SelfTestStep
import com.vpnclient.domain.Server
import com.vpnclient.domain.TunnelRepository
import com.vpnclient.domain.TunnelStatusReporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import uniffi.vpn_core.TunnAction
import uniffi.vpn_core.WireguardTunnel
import uniffi.vpn_core.generateKeypair

// The app's own client identity. In a production client this would be
// generated once on first launch and persisted, then its public half
// registered with each server out of band — not shared across servers as
// a fixed constant.
private const val CLIENT_PRIVATE_KEY = "ZgKt1TqpD3bxjdwHOrUn/6HoB9imvaLWL7fpFiGTF3s="

private fun hexPreview(bytes: ByteArray): String =
    bytes.take(8).joinToString(" ") { "%02x".format(it) } + "…"

/**
 * The single implementation of both domain contracts:
 *  - TunnelRepository — what the UI (feature modules) sees through DI;
 *  - TunnelStatusReporter — what WireguardVpnService sees (the status back-channel).
 * One class implements both interfaces deliberately: this is the single place
 * where the real connection state lives, so WireguardVpnService and the UI
 * always see the same truth.
 */
class DefaultTunnelRepository(
    private val context: Context,
) : TunnelRepository, TunnelStatusReporter {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

    override fun connectionState(): StateFlow<ConnectionState> = _connectionState

    override fun connect(server: Server) {
        val intent = Intent(context, WireguardVpnService::class.java).apply {
            putExtra(WireguardVpnService.EXTRA_PRIVATE_KEY, CLIENT_PRIVATE_KEY)
            putExtra(WireguardVpnService.EXTRA_PEER_PUBLIC_KEY, server.publicKey)
            putExtra(WireguardVpnService.EXTRA_SERVER_HOST, server.host)
            putExtra(WireguardVpnService.EXTRA_SERVER_PORT, server.port)
        }
        context.startService(intent)
    }

    override fun disconnect() {
        context.stopService(Intent(context, WireguardVpnService::class.java))
    }

    // --- TunnelStatusReporter: only WireguardVpnService writes here ---
    override fun reportConnecting() {
        _connectionState.value = ConnectionState.Connecting
    }

    override fun reportConnected() {
        _connectionState.value = ConnectionState.Connected
    }

    override fun reportDisconnected() {
        _connectionState.value = ConnectionState.Disconnected
    }

    override fun reportFailed(reason: String) {
        _connectionState.value = ConnectionState.Failed(reason)
    }

    // --- Self-test: fully offline, unrelated to the service or the network ---
    override suspend fun runSelfTest(): List<SelfTestStep> {
        val clientKeys = generateKeypair()
        val serverKeys = generateKeypair()

        val client = WireguardTunnel(clientKeys.privateKeyBase64, serverKeys.publicKeyBase64)
        val server = WireguardTunnel(serverKeys.privateKeyBase64, clientKeys.publicKeyBase64)

        val steps = mutableListOf<SelfTestStep>()
        try {
            steps += SelfTestStep("🔑 Generated client and server keys")

            val init = client.encapsulate(ByteArray(0))
            val initData = (init as? TunnAction.SendToNetwork)?.data
                ?: return steps + SelfTestStep("❌ Client did not generate a handshake init")
            steps += SelfTestStep("📤 Handshake init sent (${initData.size} bytes)", hexPreview(initData))

            val response = server.decapsulate(initData)
            val responseData = (response as? TunnAction.SendToNetwork)?.data
                ?: return steps + SelfTestStep("❌ Server did not respond to the handshake")
            steps += SelfTestStep(
                "📥 Handshake response received (${responseData.size} bytes)",
                hexPreview(responseData),
            )

            client.decapsulate(responseData)
            steps += SelfTestStep("🔒 Session established")

            val payload = "hello from self-test".toByteArray()
            val encrypted = client.encapsulate(payload)
            val encryptedData = (encrypted as? TunnAction.SendToNetwork)?.data
                ?: return steps + SelfTestStep("❌ Data was not encrypted")
            steps += SelfTestStep(
                "✉️ Encrypted ${payload.size} bytes → ${encryptedData.size} bytes",
                hexPreview(encryptedData),
            )

            steps += SelfTestStep("✅ Handshake successful")
        } finally {
            client.close()
            server.close()
        }
        return steps
    }
}
