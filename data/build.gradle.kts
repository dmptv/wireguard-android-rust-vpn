plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
}

android {
    namespace = "com.vpnclient.data"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    sourceSets["main"].java.srcDirs("src/main/kotlin")
    // The Rust .so files live at the project root — data owns the interaction
    // with the native layer, so this is the module that links them in.
    sourceSets["main"].jniLibs.srcDirs("../jniLibs")
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.jna) { artifact { type = "aar" } }
    implementation(libs.koin.android) // KoinComponent for WireguardVpnService
}
