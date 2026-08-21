package com.vpnclient.feature.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vpnclient.domain.ConnectionState
import com.vpnclient.domain.Server
import com.vpnclient.domain.ServerRepository
import com.vpnclient.domain.TunnelRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Strict MVI: the only public function is onIntent(). The state is a single
 * ConnectionState object (already sealed in :domain, no extra wrapper needed).
 */
class ConnectViewModel(
    private val repository: TunnelRepository,
    serverRepository: ServerRepository,
) : ViewModel() {

    val state: StateFlow<ConnectionState> = repository.connectionState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectionState.Disconnected)

    // Eagerly, not WhileSubscribed: nothing ever collects this flow directly —
    // onIntent only reads .value — so WhileSubscribed would never see a
    // subscriber and this would stay stuck at its initial null forever.
    private val selectedServer: StateFlow<Server?> = serverRepository.selectedServer()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun onIntent(intent: ConnectIntent) {
        when (intent) {
            ConnectIntent.Connect -> selectedServer.value?.let { repository.connect(it) }
            ConnectIntent.Disconnect -> repository.disconnect()
        }
    }
}
