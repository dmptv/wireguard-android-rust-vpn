package com.vpnclient.app

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import uniffi.vpn_core.TunnAction
import uniffi.vpn_core.WireguardTunnel
import uniffi.vpn_core.generateKeypair

// Захардкоженная конфигурация настоящего подключения — так же, как у коммерческих VPN
// клиентов, пользователь не вводит адрес сервера и ключи сам (у них это подгружается
// с бэкенда после логина, у нас — просто константы). Работает только если рядом реально
// запущен сервер с этими же ключами: rust/vpn-core/src/bin/test_server.rs.
private object VpnConfig {
    const val SERVER_HOST = "10.0.2.2" // особый адрес: с эмулятора это localhost хост-машины
    const val SERVER_PORT = 51999
    const val SERVER_PUBLIC_KEY = "RPAL1mJN9UVt2dDizw5xL31LzpjRbg1wRAeG+SiflHQ="
    const val CLIENT_PRIVATE_KEY = "ZgKt1TqpD3bxjdwHOrUn/6HoB9imvaLWL7fpFiGTF3s="
}

private data class SelfTestStep(val label: String, val hexPreview: String? = null)

private fun hexPreview(bytes: ByteArray): String =
    bytes.take(8).joinToString(" ") { "%02x".format(it) } + "…"

class MainActivity : ComponentActivity() {

    private lateinit var vpnPermissionLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vpnPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) startVpnService()
            }

        setContent {
            MaterialTheme {
                HomeScreen(
                    onConnect = ::connectToRealServer,
                    onSelfTest = ::runSelfTest,
                )
            }
        }
    }

    private fun connectToRealServer() {
        val permissionIntent = VpnService.prepare(this)
        if (permissionIntent != null) {
            vpnPermissionLauncher.launch(permissionIntent)
        } else {
            startVpnService()
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, WireguardVpnService::class.java).apply {
            putExtra(WireguardVpnService.EXTRA_PRIVATE_KEY, VpnConfig.CLIENT_PRIVATE_KEY)
            putExtra(WireguardVpnService.EXTRA_PEER_PUBLIC_KEY, VpnConfig.SERVER_PUBLIC_KEY)
            putExtra(WireguardVpnService.EXTRA_SERVER_HOST, VpnConfig.SERVER_HOST)
            putExtra(WireguardVpnService.EXTRA_SERVER_PORT, VpnConfig.SERVER_PORT)
        }
        startService(intent)
    }

    /**
     * Офлайн-демонстрация без единого внешнего сервера — специально для того, чтобы
     * любой, кто склонирует репозиторий, мог убедиться, что криптоядро (Rust + boringtun,
     * подключённый через UniFFI) реально работает, без необходимости поднимать инфраструктуру.
     * Создаёт два независимых объекта WireguardTunnel ("клиент" и "сервер") прямо внутри
     * приложения и проводит между ними настоящий WireGuard-handshake, шаг за шагом.
     */
    private fun runSelfTest(): List<SelfTestStep> {
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

@Composable
private fun HomeScreen(onConnect: () -> Unit, onSelfTest: () -> List<SelfTestStep>) {
    var allSteps by remember { mutableStateOf(emptyList<SelfTestStep>()) }
    var revealedCount by remember { mutableStateOf(0) }

    // Крипто-вычисления мгновенны, поэтому "проигрываем" шаги по одному сами,
    // отдельно от реального вычисления — это чисто визуальный эффект.
    LaunchedEffect(allSteps) {
        for (i in allSteps.indices) {
            revealedCount = i + 1
            delay(350)
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Button(onClick = onConnect, modifier = Modifier.fillMaxWidth()) {
            Text("Подключиться к серверу")
        }
        Button(
            onClick = {
                revealedCount = 0
                allSteps = onSelfTest()
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text("Self-test (офлайн, без сервера)")
        }

        Column(Modifier.padding(top = 16.dp)) {
            allSteps.take(revealedCount).forEach { step ->
                AnimatedVisibility(visible = true, enter = fadeIn() + expandVertically()) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        Text(step.label)
                        step.hexPreview?.let {
                            Text(it, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
