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
 * Строгий MVI: единственная публичная функция — onIntent(). ViewModel не знает
 * про WireguardTunnel/UniFFI вообще — только про TunnelRepository.runSelfTest(),
 * которую подставит Koin (реализация в :data).
 */
class SelfTestViewModel(
    private val repository: TunnelRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SelfTestUiState())
    val state: StateFlow<SelfTestUiState> = _state.asStateFlow()

    fun onIntent(intent: SelfTestIntent) {
        when (intent) {
            SelfTestIntent.RunSelfTest -> runSelfTest()
        }
    }

    private fun runSelfTest() {
        if (_state.value.isRunning) return
        viewModelScope.launch {
            val allSteps = repository.runSelfTest()
            _state.value = SelfTestUiState(visibleSteps = emptyList(), isRunning = true)
            // Крипто-вычисления мгновенны — "проигрываем" появление шагов сами,
            // это чисто визуальный эффект, отдельный от самого вычисления.
            for (i in allSteps.indices) {
                _state.value = _state.value.copy(visibleSteps = allSteps.take(i + 1))
                delay(350)
            }
            _state.value = _state.value.copy(isRunning = false)
        }
    }
}
