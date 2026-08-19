package com.vpnclient.feature.connect

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vpnclient.domain.ConnectionState
import org.koin.androidx.compose.koinViewModel

@Composable
fun ConnectScreen(viewModel: ConnectViewModel = koinViewModel()) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    // VpnService.prepare() needs an Activity context to show the system dialog —
    // the only place in feature-connect that has to know about the Android VPN API.
    // Once permission is granted, the user's actual intent flows to the
    // ViewModel as a regular MVI Intent — the platform step itself isn't one.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onIntent(ConnectIntent.Connect)
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            when (val s = state) {
                ConnectionState.Disconnected -> "Отключено"
                ConnectionState.Connecting -> "Подключение…"
                ConnectionState.Connected -> "✅ Подключено"
                is ConnectionState.Failed -> "❌ Ошибка: ${s.reason}"
            },
        )
        Button(
            onClick = {
                val permissionIntent = VpnService.prepare(context)
                if (permissionIntent != null) {
                    permissionLauncher.launch(permissionIntent)
                } else {
                    viewModel.onIntent(ConnectIntent.Connect)
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text("Подключиться к серверу")
        }
        Button(
            onClick = { viewModel.onIntent(ConnectIntent.Disconnect) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text("Отключиться")
        }
    }
}
