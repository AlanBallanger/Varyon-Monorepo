plugins {
    `maven-publish`
    idea
    id("hytale-mod") version "0.+"
}

group = "fr.varyon"
version = properties["plugin_version"] as String
val javaVersion = 25

repositories {
    mavenCentral()
    maven("https://maven.hytale-modding.info/releases") { name = "HytaleModdingReleases" }
    maven("https://maven.hytale-modding.info/pre-release") { name = "HytaleModdingPreRelease" }
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.1.0")
    compileOnly("org.jspecify:jspecify:0.3.0")
}

hytale {
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(javaVersion) }
}

tasks.named<ProcessResources>("processResources") {
    val replaceProperties = mapOf(
        "plugin_group"           to findProperty("plugin_group"),
        "plugin_maven_group"     to project.group,
        "plugin_name"            to (findProperty("plugin_name") ?: project.name),
        "plugin_version"         to project.version,
        "server_version"         to findProperty("server_version"),
        "plugin_description"     to findProperty("plugin_description"),
        "plugin_website"         to findProperty("plugin_website"),
        "plugin_main_entrypoint" to findProperty("plugin_main_entrypoint"),
        "plugin_author"          to findProperty("plugin_author"),
    )
    filesMatching("manifest.json") { expand(replaceProperties) }
    inputs.properties(replaceProperties)
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

publishing {
    repositories {}
    publications { create<MavenPublication>("maven") { from(components["java"]) } }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
