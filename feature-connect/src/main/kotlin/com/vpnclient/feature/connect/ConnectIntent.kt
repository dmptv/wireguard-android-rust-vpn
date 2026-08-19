package com.vpnclient.feature.connect

/**
 * The system VPN permission dialog (VpnService.prepare()) is not an Intent —
 * it's a platform-level step BEFORE the user's actual intent: it's either
 * skipped (permission already granted) or blocks Connect until confirmed in
 * the dialog. That's why ConnectScreen itself decides when to actually
 * dispatch Connect to the ViewModel — this doesn't break MVI, the platform
 * consent step simply lives outside the Intent flow.
 */
sealed interface ConnectIntent {
    data object Connect : ConnectIntent
    data object Disconnect : ConnectIntent
}
