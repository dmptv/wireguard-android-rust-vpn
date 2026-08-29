package com.vpnclient.feature.servers

import com.vpnclient.domain.Server
import com.vpnclient.domain.ServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class ServerListViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val serverA = Server(id = "a", name = "Server A", host = "1.1.1.1", port = 51820, publicKey = "keyA")
    private val serverB = Server(id = "b", name = "Server B", host = "2.2.2.2", port = 51820, publicKey = "keyB")

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `state combines servers and the current selection`() = runTest(dispatcher) {
        val repository = mock<ServerRepository> {
            on { servers() } doReturn MutableStateFlow(listOf(serverA, serverB))
            on { selectedServer() } doReturn MutableStateFlow(serverA)
        }

        val viewModel = ServerListViewModel(repository)
        // state is a WhileSubscribed StateFlow — it only starts combining
        // upstream flows once it has a collector, so we need one active here.
        val collectorJob = launch { viewModel.state.collect {} }
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        collectorJob.cancel()
        assertEquals(listOf(serverA, serverB), state.servers)
        assertEquals(serverA, state.selectedServer)
    }

    @Test
    fun `SelectServer intent delegates to the repository`() {
        val repository = mock<ServerRepository> {
            on { servers() } doReturn MutableStateFlow(listOf(serverA, serverB))
            on { selectedServer() } doReturn MutableStateFlow(serverA)
        }
        val viewModel = ServerListViewModel(repository)

        viewModel.onIntent(ServerListIntent.SelectServer(serverB))

        verify(repository).selectServer(serverB)
    }
}
