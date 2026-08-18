package com.vpnclient.app

import android.app.Application
import com.vpnclient.data.DefaultTunnelRepository
import com.vpnclient.domain.TunnelRepository
import com.vpnclient.domain.TunnelStatusReporter
import com.vpnclient.feature.connect.ConnectViewModel
import com.vpnclient.feature.selftest.SelfTestViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.binds
import org.koin.dsl.module

/**
 * Единственное место во всём проекте, где известны конкретные реализации —
 * feature-модули и WireguardVpnService видят только интерфейсы из :domain.
 * DefaultTunnelRepository регистрируется сразу под ДВУМЯ типами (`binds`),
 * потому что это один и тот же объект и для UI (TunnelRepository), и для
 * сервиса, который сообщает статус (TunnelStatusReporter) — состояние общее.
 */
private val appModule = module {
    single {
        DefaultTunnelRepository(androidContext())
    } binds arrayOf(TunnelRepository::class, TunnelStatusReporter::class)

    viewModel { SelfTestViewModel(get()) }
    viewModel { ConnectViewModel(get()) }
}

class VpnClientApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@VpnClientApp)
            modules(appModule)
        }
    }
}
