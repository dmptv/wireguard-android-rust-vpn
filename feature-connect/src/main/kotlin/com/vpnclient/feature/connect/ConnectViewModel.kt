package com.vpnclient.feature.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vpnclient.domain.ConnectionState
import com.vpnclient.domain.TunnelRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Как и SelfTestViewModel, ничего не знает про VpnService или Android VPN API
 * напрямую — только про TunnelRepository. Разрешение VPN (системный диалог)
 * обрабатывает сам экран (ConnectScreen), потому что запуск диалога требует
 * Activity-контекста — это единственная часть, которую по-честному нельзя
 * вынести из UI-слоя.
 */
class ConnectViewModel(
    private val repository: TunnelRepository,
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = repository.connectionState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectionState.Disconnected)

    fun onPermissionGranted() = repository.connect()

    fun disconnect() = repository.disconnect()
}
