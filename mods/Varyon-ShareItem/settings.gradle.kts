pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.hytale-modding.info/releases") {
            name = "HytaleModdingReleases"
        }
        maven("https://maven.hytale-modding.info/pre-release") {
            name = "HytaleModdingPreRelease"
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "Varyon-ShareItem"
