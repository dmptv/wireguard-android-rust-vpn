package com.vpnclient.feature.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vpnclient.domain.ConnectionState
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
) : ViewModel() {

    val state: StateFlow<ConnectionState> = repository.connectionState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectionState.Disconnected)

    fun onIntent(intent: ConnectIntent) {
        when (intent) {
            ConnectIntent.Connect -> repository.connect()
            ConnectIntent.Disconnect -> repository.disconnect()
        }
    }
}
