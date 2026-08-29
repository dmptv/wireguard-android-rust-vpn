package com.vpnclient.data

import android.app.Application
import com.vpnclient.domain.ConnectionState
import com.vpnclient.domain.DataError
import com.vpnclient.domain.Result
import com.vpnclient.domain.Server
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import uniffi.vpn_core.TunnAction
import uniffi.vpn_core.WireguardTunnel

@RunWith(RobolectricTestRunner::class)
class DefaultTunnelRepositoryTest {

    private lateinit var context: Application
    private lateinit var repository: DefaultTunnelRepository

    private val server = Server(
        id = "test",
        name = "Test server",
        host = "10.0.2.2",
        port = 51999,
        publicKey = "peer-public-key",
    )

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        repository = DefaultTunnelRepository(context)
    }

    @Test
    fun `connect starts WireguardVpnService with the server's connection details`() {
        repository.connect(server)

        val started = shadowOf(context).nextStartedService
        assertEquals(WireguardVpnService::class.java.name, started.component?.className)
        assertEquals(server.publicKey, started.getStringExtra(WireguardVpnService.EXTRA_PEER_PUBLIC_KEY))
        assertEquals(server.host, started.getStringExtra(WireguardVpnService.EXTRA_SERVER_HOST))
        assertEquals(server.port, started.getIntExtra(WireguardVpnService.EXTRA_SERVER_PORT, -1))
    }

    @Test
    fun `disconnect stops WireguardVpnService`() {
        repository.connect(server)
        shadowOf(context).clearStartedServices()

        repository.disconnect()

        val stopped = shadowOf(context).nextStoppedService
        assertEquals(WireguardVpnService::class.java.name, stopped?.component?.className)
    }

    @Test
    fun `connectionState starts Disconnected and reflects status reports`() {
        assertEquals(ConnectionState.Disconnected, repository.connectionState().value)

        repository.reportConnecting()
        assertEquals(ConnectionState.Connecting, repository.connectionState().value)

        repository.reportConnected()
        assertEquals(ConnectionState.Connected, repository.connectionState().value)

        repository.reportFailed("handshake timeout")
        val failed = repository.connectionState().value as ConnectionState.Failed
        assertEquals("handshake timeout", failed.reason)

        repository.reportDisconnected()
        assertEquals(ConnectionState.Disconnected, repository.connectionState().value)
    }

    @Test
    fun `runSelfTest returns Success with every step when the handshake completes`() = runTest {
        val client = mock<WireguardTunnel>()
        val server = mock<WireguardTunnel>()
        whenever(client.encapsulate(any())).thenReturn(
            TunnAction.SendToNetwork(byteArrayOf(1)),
            TunnAction.SendToNetwork(byteArrayOf(3)),
        )
        whenever(server.decapsulate(any())).thenReturn(TunnAction.SendToNetwork(byteArrayOf(2)))
        whenever(client.decapsulate(any())).thenReturn(TunnAction.Nothing)
        val repositoryUnderTest = DefaultTunnelRepository(context) { client to server }

        val result = repositoryUnderTest.runSelfTest()

        check(result is Result.Success)
        assertEquals(6, result.data.size)
        verify(client).close()
        verify(server).close()
    }

    @Test
    fun `runSelfTest returns HANDSHAKE_FAILED when the client does not produce a handshake init`() = runTest {
        val client = mock<WireguardTunnel>()
        val server = mock<WireguardTunnel>()
        whenever(client.encapsulate(any())).thenReturn(TunnAction.Nothing)
        val repositoryUnderTest = DefaultTunnelRepository(context) { client to server }

        val result = repositoryUnderTest.runSelfTest()

        assertEquals(Result.Error(DataError.Local.HANDSHAKE_FAILED), result)
        verify(client).close()
        verify(server).close()
    }

    @Test
    fun `runSelfTest returns HANDSHAKE_FAILED when the server does not respond to the handshake`() = runTest {
        val client = mock<WireguardTunnel>()
        val server = mock<WireguardTunnel>()
        whenever(client.encapsulate(any())).thenReturn(TunnAction.SendToNetwork(byteArrayOf(1)))
        whenever(server.decapsulate(any())).thenReturn(TunnAction.Nothing)
        val repositoryUnderTest = DefaultTunnelRepository(context) { client to server }

        val result = repositoryUnderTest.runSelfTest()

        assertEquals(Result.Error(DataError.Local.HANDSHAKE_FAILED), result)
        verify(client).close()
        verify(server).close()
    }

    @Test
    fun `runSelfTest returns HANDSHAKE_FAILED when the payload is not encrypted`() = runTest {
        val client = mock<WireguardTunnel>()
        val server = mock<WireguardTunnel>()
        whenever(client.encapsulate(any())).thenReturn(
            TunnAction.SendToNetwork(byteArrayOf(1)),
            TunnAction.Nothing,
        )
        whenever(server.decapsulate(any())).thenReturn(TunnAction.SendToNetwork(byteArrayOf(2)))
        whenever(client.decapsulate(any())).thenReturn(TunnAction.Nothing)
        val repositoryUnderTest = DefaultTunnelRepository(context) { client to server }

        val result = repositoryUnderTest.runSelfTest()

        assertEquals(Result.Error(DataError.Local.HANDSHAKE_FAILED), result)
        verify(client).close()
        verify(server).close()
    }
}
