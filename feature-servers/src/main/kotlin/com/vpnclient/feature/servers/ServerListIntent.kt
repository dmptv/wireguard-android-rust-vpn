package com.vpnclient.feature.servers

import com.vpnclient.domain.Server

sealed interface ServerListIntent {
    data class SelectServer(val server: Server) : ServerListIntent
}
