package com.vpnclient.domain

/**
 * A single step of the offline handshake demo. The presentation layer
 * (feature-selftest) knows nothing about WireGuard or UniFFI — only this
 * plain model.
 */
data class SelfTestStep(
    val label: String,
    val hexPreview: String? = null,
)
