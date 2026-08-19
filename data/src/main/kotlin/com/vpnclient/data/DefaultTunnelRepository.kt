package com.vpnclient.data

import android.content.Context
import android.content.Intent
import com.vpnclient.domain.ConnectionState
import com.vpnclient.domain.SelfTestStep
import com.vpnclient.domain.TunnelRepository
import com.vpnclient.domain.TunnelStatusReporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import uniffi.vpn_core.TunnAction
import uniffi.vpn_core.WireguardTunnel
import uniffi.vpn_core.generateKeypair

// Hardcoded configuration for the real connection — as with commercial VPN
// clients, the user never enters a server address or keys. Only works while
// a server with these same keys is actually running: rust/vpn-core/src/bin/test_server.rs.
private object VpnConfig {
    const val SERVER_HOST = "10.0.2.2"
    const val SERVER_PORT = 51999
    const val SERVER_PUBLIC_KEY = "RPAL1mJN9UVt2dDizw5xL31LzpjRbg1wRAeG+SiflHQ="
    const val CLIENT_PRIVATE_KEY = "ZgKt1TqpD3bxjdwHOrUn/6HoB9imvaLWL7fpFiGTF3s="
}

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

    override fun connect() {
        val intent = Intent(context, WireguardVpnService::class.java).apply {
            putExtra(WireguardVpnService.EXTRA_PRIVATE_KEY, VpnConfig.CLIENT_PRIVATE_KEY)
            putExtra(WireguardVpnService.EXTRA_PEER_PUBLIC_KEY, VpnConfig.SERVER_PUBLIC_KEY)
            putExtra(WireguardVpnService.EXTRA_SERVER_HOST, VpnConfig.SERVER_HOST)
            putExtra(WireguardVpnService.EXTRA_SERVER_PORT, VpnConfig.SERVER_PORT)
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
            steps += SelfTestStep("🔑 Сгенерированы ключи клиента и сервера")

            val init = client.encapsulate(ByteArray(0))
            val initData = (init as? TunnAction.SendToNetwork)?.data
                ?: return steps + SelfTestStep("❌ Клиент не сгенерировал handshake init")
            steps += SelfTestStep("📤 Handshake init отправлен (${initData.size} байт)", hexPreview(initData))

            val response = server.decapsulate(initData)
            val responseData = (response as? TunnAction.SendToNetwork)?.data
                ?: return steps + SelfTestStep("❌ Сервер не ответил на handshake")
            steps += SelfTestStep(
                "📥 Handshake response получен (${responseData.size} байт)",
                hexPreview(responseData),
            )

            client.decapsulate(responseData)
            steps += SelfTestStep("🔒 Сессия установлена")

            val payload = "hello from self-test".toByteArray()
            val encrypted = client.encapsulate(payload)
            val encryptedData = (encrypted as? TunnAction.SendToNetwork)?.data
                ?: return steps + SelfTestStep("❌ Данные не зашифровались")
            steps += SelfTestStep(
                "✉️ Зашифровано ${payload.size} байт → ${encryptedData.size} байт",
                hexPreview(encryptedData),
            )

            steps += SelfTestStep("✅ Handshake успешен")
        } finally {
            client.close()
            server.close()
        }
        return steps
    }
}
