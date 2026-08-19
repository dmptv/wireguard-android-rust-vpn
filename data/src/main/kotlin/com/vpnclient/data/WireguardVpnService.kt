package com.vpnclient.data

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import com.vpnclient.domain.TunnelStatusReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import uniffi.vpn_core.TunnAction
import uniffi.vpn_core.WireguardTunnel
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

/**
 * The real VpnService. It reports its actual status through
 * TunnelStatusReporter — the repository (:data/DefaultTunnelRepository)
 * implements this interface, and Koin injects it here, so the service never
 * depends on the concrete repository class.
 */
class WireguardVpnService : VpnService() {

    private val statusReporter: TunnelStatusReporter by inject()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var tunFd: ParcelFileDescriptor? = null
    private var socket: DatagramSocket? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val privateKey = intent?.getStringExtra(EXTRA_PRIVATE_KEY) ?: return START_NOT_STICKY
        val peerPublicKey = intent.getStringExtra(EXTRA_PEER_PUBLIC_KEY) ?: return START_NOT_STICKY
        val serverHost = intent.getStringExtra(EXTRA_SERVER_HOST) ?: return START_NOT_STICKY
        val serverPort = intent.getIntExtra(EXTRA_SERVER_PORT, 0)

        statusReporter.reportConnecting()
        startTunnel(privateKey, peerPublicKey, serverHost, serverPort)
        return START_STICKY
    }

    private fun startTunnel(privateKey: String, peerPublicKey: String, host: String, port: Int) {
        val pfd = try {
            Builder()
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .setMtu(1280)
                .setSession("VpnClient")
                .establish()
        } catch (e: Exception) {
            statusReporter.reportFailed(e.message ?: "failed to establish TUN interface")
            return
        }
        if (pfd == null) {
            statusReporter.reportFailed("VpnService.Builder.establish() returned null")
            return
        }
        tunFd = pfd

        val udp = DatagramSocket()
        // Critical: without protect(), the system would route this very socket
        // back through our own TUN interface — an infinite loop.
        protect(udp)
        udp.connect(InetSocketAddress(host, port))
        socket = udp

        val tunnel = WireguardTunnel(privateKey, peerPublicKey)

        val tunIn = FileInputStream(pfd.fileDescriptor)
        val tunOut = FileOutputStream(pfd.fileDescriptor)

        // Coroutine 1: packets from apps on the phone (TUN) -> encrypt -> send to the server.
        scope.launch {
            val buffer = ByteArray(2048)
            while (true) {
                val length = tunIn.read(buffer)
                if (length <= 0) continue
                val packet = buffer.copyOf(length)
                handleAction(tunnel.encapsulate(packet), udp)
            }
        }

        // Coroutine 2: packets from the server -> decrypt -> write back to TUN.
        scope.launch {
            val buffer = ByteArray(2048)
            while (true) {
                val datagramPacket = DatagramPacket(buffer, buffer.size)
                udp.receive(datagramPacket)
                val data = buffer.copyOf(datagramPacket.length)
                when (val action = tunnel.decapsulate(data)) {
                    is TunnAction.WriteToTunnel -> {
                        tunOut.write(action.data)
                        statusReporter.reportConnected() // first decrypted data = the session is actually working
                    }
                    is TunnAction.SendToNetwork -> udp.send(DatagramPacket(action.data, action.data.size))
                    TunnAction.Nothing -> Unit
                }
            }
        }

        // Coroutine 3: once a second — keepalive and handshake retries.
        scope.launch {
            while (true) {
                delay(1000)
                handleAction(tunnel.tick(), udp)
            }
        }

        handleAction(tunnel.encapsulate(ByteArray(0)), udp)
    }

    private fun handleAction(action: TunnAction, udp: DatagramSocket) {
        if (action is TunnAction.SendToNetwork) {
            udp.send(DatagramPacket(action.data, action.data.size))
        }
    }

    override fun onDestroy() {
        scope.cancel()
        socket?.close()
        tunFd?.close()
        statusReporter.reportDisconnected()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_PRIVATE_KEY = "private_key"
        const val EXTRA_PEER_PUBLIC_KEY = "peer_public_key"
        const val EXTRA_SERVER_HOST = "server_host"
        const val EXTRA_SERVER_PORT = "server_port"
    }
}
