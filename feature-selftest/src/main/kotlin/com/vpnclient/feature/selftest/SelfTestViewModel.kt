package com.vpnclient.feature.selftest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vpnclient.domain.Result
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
 * Strict MVI: the only public function is onIntent(). The ViewModel knows
 * nothing about WireguardTunnel or UniFFI — only about
 * TunnelRepository.runSelfTest(), whose implementation Koin injects (from :data).
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
            when (val result = repository.runSelfTest()) {
                is Result.Success -> {
                    _state.value = SelfTestUiState(visibleSteps = emptyList(), isRunning = true)
                    // The crypto itself is instant — we stagger the reveal of
                    // steps ourselves, purely as a visual effect separate
                    // from the computation.
                    val allSteps = result.data
                    for (i in allSteps.indices) {
                        _state.value = _state.value.copy(visibleSteps = allSteps.take(i + 1))
                        delay(350)
                    }
                    _state.value = _state.value.copy(isRunning = false)
                }
                is Result.Error -> {
                    _state.value = SelfTestUiState(
                        visibleSteps = listOf(SelfTestStep("❌ Self-test failed: ${result.error}")),
                        isRunning = false,
                    )
                }
            }
        }
    }
}
