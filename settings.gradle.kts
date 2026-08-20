pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.hytale-modding.info/releases") {
            name = "HytaleModdingReleases"
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "Varyon-Monorepo"

include(
    ":mods:Varyon",
    ":mods:Varyon-ShareItem",
    ":mods:Varyon-BossArena",
    ":mods:Varyon-Comet",
    ":mods:Varyon-EasyHunger",
    ":mods:Varyon-Damage_Number",
    ":mods:Varyon-Ecotale",
    ":mods:Varyon-Logs",
    ":mods:Varyon-Logs-Early",
    ":mods:Varyon-MapMarker",
    ":mods:Varyon-MusicZones",
    ":mods:Varyon-PlayerMarker",
    ":mods:Varyon-Playtime",
    ":mods:Varyon-RPG",
    ":mods:Varyon-SignaturePreservation",
    ":mods:Varyon-World-Manager",
    ":mods:varyon-UI",
    ":mods:Varyon-Progression",
    ":mods:Varyon-Holograms",
    ":mods:Varyon-TravelingCamera",
    ":mods:Varyon-CrashRestarter",
    ":mods:Varyon-TpToWorld",
    ":mods:Varyon-CraftRestrict",
    ":mods:WeaponStatsViewer",
    ":mods:Varyon-ExtendedTeleporters",
    ":mods:Varyon-KillCommand",
    ":mods:Varyon-Death",
    ":mods:Varyon-ReadableBooks",
    ":mods:Varyon-Quiver",
    ":mods:Varyon-Shop",
    ":mods:Varyon-Dummy",
    ":mods:Mixin-Varyon-StackSize",
    ":mods:Varyon-StackTiers",
)
