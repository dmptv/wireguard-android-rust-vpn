package com.vpnclient.feature.selftest

/**
 * Единственный вход в ViewModel — UI не вызывает методы напрямую (viewModel.foo()),
 * а отправляет Intent через viewModel.onIntent(...). Сейчас действие одно,
 * но по мере роста приложения новые действия — это новые case здесь,
 * а не новые публичные методы ViewModel.
 */
sealed interface SelfTestIntent {
    data object RunSelfTest : SelfTestIntent
}
