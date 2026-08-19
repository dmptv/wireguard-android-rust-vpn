package com.vpnclient.feature.servers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun ServerListScreen(viewModel: ServerListViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(Modifier.fillMaxSize()) {
        items(state.servers) { server ->
            val isSelected = server.id == state.selectedServer?.id
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.onIntent(ServerListIntent.SelectServer(server)) }
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                    )
                    .padding(16.dp),
            ) {
                Text(server.name, style = MaterialTheme.typography.titleMedium)
                Text("${server.host}:${server.port}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
