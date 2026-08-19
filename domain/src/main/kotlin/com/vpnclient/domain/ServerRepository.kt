package com.vpnclient.domain

import kotlinx.coroutines.flow.Flow

/**
 * Manages the list of available servers and which one is currently selected.
 * Kept separate from TunnelRepository because it has a different
 * responsibility: choosing a server is unrelated to establishing or
 * monitoring a tunnel connection.
 */
interface ServerRepository {
    fun servers(): Flow<List<Server>>
    fun selectedServer(): Flow<Server?>
    fun selectServer(server: Server)
}
