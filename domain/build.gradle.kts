// A plain Kotlin JVM module, with NO com.android.* plugins and no Android SDK.
// This is an architectural guarantee: domain cannot accidentally start
// depending on Context, Activity, or anything else Android-specific.
plugins {
    alias(libs.plugins.kotlinJvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // core, not the android variant — domain must not pull in the Android SDK.
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit5.jupiter)
    testRuntimeOnly(libs.junit5.jupiter.engine)
}

tasks.test {
    useJUnitPlatform()
}
