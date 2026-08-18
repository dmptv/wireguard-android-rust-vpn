package com.vpnclient.feature.selftest

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun SelfTestScreen(viewModel: SelfTestViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Button(
            onClick = viewModel::runSelfTest,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Self-test (офлайн, без сервера)")
        }

        Column(Modifier.padding(top = 16.dp)) {
            uiState.visibleSteps.forEach { step ->
                AnimatedVisibility(visible = true, enter = fadeIn() + expandVertically()) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        Text(step.label)
                        step.hexPreview?.let {
                            Text(it, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
