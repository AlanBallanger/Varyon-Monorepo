plugins {
    `maven-publish`
    idea
    id("hytale-mod") version "0.+"
}

group = "fr.varyon"
version = "1.0.0"
val javaVersion = 25

repositories {
    mavenCentral()
    maven("https://maven.hytale-modding.info/releases") {
        name = "HytaleModdingReleases"
    }
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.1.0")
    compileOnly("org.jspecify:jspecify:0.3.0")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
}

hytale {
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(javaVersion) }
    withSourcesJar()
}

fun requiredProperty(name: String): String =
    findProperty(name)?.toString()?.takeIf { it.isNotBlank() }
        ?: throw GradleException("Missing gradle.properties entry: $name")

tasks.named<ProcessResources>("processResources") {
    val replaceProperties = mapOf(
        "plugin_group" to requiredProperty("plugin_group"),
        "plugin_maven_group" to project.group,
        "plugin_name" to (findProperty("plugin_name")?.toString()?.takeIf { it.isNotBlank() } ?: project.name),
        "plugin_version" to project.version,
        "server_version" to requiredProperty("server_version"),
        "plugin_description" to requiredProperty("plugin_description"),
        "plugin_website" to findProperty("plugin_website")?.toString().orEmpty(),
        "plugin_main_entrypoint" to requiredProperty("plugin_main_entrypoint"),
        "plugin_author" to requiredProperty("plugin_author"),
    )
    filesMatching("manifest.json") { expand(replaceProperties) }
    inputs.properties(replaceProperties)
}

val fatJar = tasks.register<Zip>("fatJar") {
    archiveBaseName.set("Varyon-Shop")
    archiveVersion.set(version.toString())
    archiveExtension.set("jar")
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output)

    exclude("META-INF/versions/**")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    exclude("META-INF/*.SF")
    exclude("module-info.class")
    exclude("**/package-info.class")
}

tasks.named("build") {
    dependsOn(fatJar)
}

tasks.named<Jar>("jar") {
    archiveClassifier.set("thin")
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
