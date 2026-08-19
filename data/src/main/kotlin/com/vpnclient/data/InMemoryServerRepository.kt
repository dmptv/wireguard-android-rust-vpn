package com.vpnclient.data

import com.vpnclient.domain.Server
import com.vpnclient.domain.ServerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// The single server currently reachable — matches the fixed key pair in
// rust/vpn-core/src/bin/test_server.rs. Additional entries can be added
// here once more servers are actually running.
private val defaultServers = listOf(
    Server(
        id = "local-dev",
        name = "Local development server",
        host = "10.0.2.2",
        port = 51999,
        publicKey = "RPAL1mJN9UVt2dDizw5xL31LzpjRbg1wRAeG+SiflHQ=",
    ),
)

class InMemoryServerRepository : ServerRepository {

    private val _servers = MutableStateFlow(defaultServers)
    private val _selectedServer = MutableStateFlow(defaultServers.firstOrNull())

    override fun servers(): StateFlow<List<Server>> = _servers

    override fun selectedServer(): StateFlow<Server?> = _selectedServer

    override fun selectServer(server: Server) {
        _selectedServer.value = server
    }
}
