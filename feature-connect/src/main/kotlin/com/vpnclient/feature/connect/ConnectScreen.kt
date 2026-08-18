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
    val state by viewModel.connectionState.collectAsStateWithLifecycle()

    // VpnService.prepare() требует Activity-контекста для показа системного диалога —
    // единственное место во всём feature-connect, где приходится знать про Android VPN API.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onPermissionGranted()
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
                    viewModel.onPermissionGranted()
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text("Подключиться к серверу")
        }
        Button(
            onClick = viewModel::disconnect,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text("Отключиться")
        }
    }
}
