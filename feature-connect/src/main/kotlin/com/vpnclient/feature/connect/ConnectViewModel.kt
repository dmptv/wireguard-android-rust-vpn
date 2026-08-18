package com.vpnclient.feature.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vpnclient.domain.ConnectionState
import com.vpnclient.domain.TunnelRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Строгий MVI: единственная публичная функция — onIntent(). Состояние — один
 * объект ConnectionState (уже sealed в :domain, дополнительная обёртка не нужна).
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
