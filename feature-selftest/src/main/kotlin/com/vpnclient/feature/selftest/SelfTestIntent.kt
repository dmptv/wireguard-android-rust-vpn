package com.vpnclient.feature.selftest

/**
 * The single entry point into the ViewModel — the UI never calls methods
 * directly (viewModel.foo()), it dispatches an Intent via
 * viewModel.onIntent(...). There's only one action today, but as the app
 * grows, new actions become new cases here, not new public ViewModel methods.
 */
sealed interface SelfTestIntent {
    data object RunSelfTest : SelfTestIntent
}
