// Чистый Kotlin JVM модуль, БЕЗ com.android.* плагинов и без Android SDK.
// Это архитектурная гарантия: domain не может случайно начать зависеть
// от Context, Activity или чего-либо ещё специфичного для Android.
plugins {
    alias(libs.plugins.kotlinJvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // core, не android-вариант — domain не должен тянуть Android SDK.
    implementation(libs.kotlinx.coroutines.core)
}
