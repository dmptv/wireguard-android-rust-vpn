package com.vpnclient.feature.connect

/**
 * Системный диалог разрешения VPN (VpnService.prepare()) — не Intent, а
 * платформенный шаг ДО намерения пользователя: он либо пропускается (разрешение
 * уже выдано), либо блокирует Connect до подтверждения в диалоге. Поэтому
 * ConnectScreen сам решает, когда реально отправить Connect в ViewModel —
 * это не нарушает MVI, просто платформенное согласие живёт снаружи Intent-потока.
 */
sealed interface ConnectIntent {
    data object Connect : ConnectIntent
    data object Disconnect : ConnectIntent
}
