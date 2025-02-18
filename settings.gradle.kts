enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
    repositories {
        maven("https://maven.myket.ir")
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://maven.myket.ir")
        google()
        mavenCentral()
    }
}

rootProject.name = "Komposer"
include(":androidApp")
include(":shared")