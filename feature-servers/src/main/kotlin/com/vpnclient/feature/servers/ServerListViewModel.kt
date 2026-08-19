package com.vpnclient.feature.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vpnclient.domain.Server
import com.vpnclient.domain.ServerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ServerListUiState(
    val servers: List<Server> = emptyList(),
    val selectedServer: Server? = null,
)

/**
 * Strict MVI, same shape as SelfTestViewModel/ConnectViewModel: a single
 * onIntent() entry point and a single state object combining the server
 * list with the current selection.
 */
class ServerListViewModel(
    private val repository: ServerRepository,
) : ViewModel() {

    val state: StateFlow<ServerListUiState> = combine(
        repository.servers(),
        repository.selectedServer(),
    ) { servers, selected ->
        ServerListUiState(servers = servers, selectedServer = selected)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ServerListUiState())

    fun onIntent(intent: ServerListIntent) {
        when (intent) {
            is ServerListIntent.SelectServer -> repository.selectServer(intent.server)
        }
    }
}
