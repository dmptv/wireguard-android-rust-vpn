package com.vpnclient.domain

data class Server(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val publicKey: String,
)
