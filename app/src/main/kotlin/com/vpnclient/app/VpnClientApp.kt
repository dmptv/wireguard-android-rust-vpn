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
 * The only place in the whole project that knows about concrete
 * implementations — feature modules and WireguardVpnService only ever see
 * interfaces from :domain. DefaultTunnelRepository is registered under BOTH
 * types (`binds`) because it's the same object serving the UI
 * (TunnelRepository) and the service that reports status
 * (TunnelStatusReporter) — the state is shared.
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
