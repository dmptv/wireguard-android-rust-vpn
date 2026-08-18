package com.vpnclient.feature.selftest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vpnclient.domain.SelfTestStep
import com.vpnclient.domain.TunnelRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SelfTestUiState(
    val visibleSteps: List<SelfTestStep> = emptyList(),
    val isRunning: Boolean = false,
)

/**
 * ViewModel не знает про WireguardTunnel/UniFFI вообще — только про
 * TunnelRepository.runSelfTest(), которую подставит Koin (реализация в :data).
 * Это и есть смысл границы feature | domain | data: экран тестируем без Android
 * и без Rust, подставив fake TunnelRepository.
 */
class SelfTestViewModel(
    private val repository: TunnelRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SelfTestUiState())
    val uiState: StateFlow<SelfTestUiState> = _uiState.asStateFlow()

    fun runSelfTest() {
        if (_uiState.value.isRunning) return
        viewModelScope.launch {
            val allSteps = repository.runSelfTest()
            _uiState.value = SelfTestUiState(visibleSteps = emptyList(), isRunning = true)
            // Крипто-вычисления мгновенны — "проигрываем" появление шагов сами,
            // это чисто визуальный эффект, отдельный от самого вычисления.
            for (i in allSteps.indices) {
                _uiState.value = _uiState.value.copy(visibleSteps = allSteps.take(i + 1))
                delay(350)
            }
            _uiState.value = _uiState.value.copy(isRunning = false)
        }
    }
}
