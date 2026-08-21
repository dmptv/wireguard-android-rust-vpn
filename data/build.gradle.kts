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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    sourceSets["main"].java.srcDirs("src/main/kotlin")
    // The Rust .so files live at the project root — data owns the interaction
    // with the native layer, so this is the module that links them in.
    sourceSets["main"].jniLibs.srcDirs("../jniLibs")
    sourceSets["test"].java.srcDirs("src/test/kotlin")

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { it.useJUnitPlatform() }
        }
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.jna) { artifact { type = "aar" } }
    implementation(libs.koin.android) // KoinComponent for WireguardVpnService

    // DefaultTunnelRepository touches android.content.Context/Intent, so its
    // tests run under Robolectric (JVM-simulated Android) rather than as a
    // plain unit test. Robolectric tests stay JUnit4-style; the vintage
    // engine lets them run on the same JUnit5 platform as the rest of the module.
    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testRuntimeOnly(libs.junit5.vintage.engine)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.kotlinx.coroutines.test)
}
