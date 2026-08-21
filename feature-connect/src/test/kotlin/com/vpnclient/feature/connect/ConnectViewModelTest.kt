package com.vpnclient.feature.connect

import com.vpnclient.domain.ConnectionState
import com.vpnclient.domain.Server
import com.vpnclient.domain.ServerRepository
import com.vpnclient.domain.TunnelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val server = Server(id = "a", name = "Server A", host = "1.1.1.1", port = 51820, publicKey = "keyA")

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Connect intent connects to the selected server`() {
        val tunnelRepository = mock<TunnelRepository> {
            on { connectionState() } doReturn MutableStateFlow(ConnectionState.Disconnected)
        }
        val serverRepository = mock<ServerRepository> {
            on { selectedServer() } doReturn MutableStateFlow(server)
        }
        val viewModel = ConnectViewModel(tunnelRepository, serverRepository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(ConnectIntent.Connect)

        verify(tunnelRepository).connect(server)
    }

    @Test
    fun `Connect intent does nothing when no server is selected`() {
        val tunnelRepository = mock<TunnelRepository> {
            on { connectionState() } doReturn MutableStateFlow(ConnectionState.Disconnected)
        }
        val serverRepository = mock<ServerRepository> {
            on { selectedServer() } doReturn MutableStateFlow(null)
        }
        val viewModel = ConnectViewModel(tunnelRepository, serverRepository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(ConnectIntent.Connect)

        verify(tunnelRepository, never()).connect(org.mockito.kotlin.any())
    }

    @Test
    fun `Disconnect intent disconnects unconditionally`() {
        val tunnelRepository = mock<TunnelRepository> {
            on { connectionState() } doReturn MutableStateFlow(ConnectionState.Connected)
        }
        val serverRepository = mock<ServerRepository> {
            on { selectedServer() } doReturn MutableStateFlow(null)
        }
        val viewModel = ConnectViewModel(tunnelRepository, serverRepository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(ConnectIntent.Disconnect)

        verify(tunnelRepository).disconnect()
    }
}
