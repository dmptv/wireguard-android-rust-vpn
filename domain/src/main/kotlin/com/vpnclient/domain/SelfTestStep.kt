package com.vpnclient.domain

/**
 * Один шаг офлайн-демонстрации handshake. Presentation-слой (feature-selftest)
 * ничего не знает о WireGuard/UniFFI — только об этой чистой модели.
 */
data class SelfTestStep(
    val label: String,
    val hexPreview: String? = null,
)
