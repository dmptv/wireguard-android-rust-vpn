pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "VpnClient"
include(":app")
include(":domain")
include(":data")
include(":feature-selftest")
include(":feature-connect")
include(":feature-servers")
