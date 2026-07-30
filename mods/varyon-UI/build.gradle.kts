import org.gradle.api.tasks.bundling.Zip

plugins {
    java
}

group = properties["plugin_group"] as String
version = properties["plugin_version"] as String

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.1.0")
    compileOnly("org.jspecify:jspecify:0.3.0")
    compileOnly(files("../../libs/HytaleServer.jar"))
    implementation("com.moandjiezana.toml:toml4j:0.7.2")
    implementation("com.google.code.gson:gson:2.10.1")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.named<ProcessResources>("processResources") {
    filesMatching("manifest.json") { expand(project.properties) }
}

tasks.named<Jar>("jar") {
    enabled = false
}

// Standard Jar task was found to intermittently/consistently fail to write its output file in
// this environment (Java 25 + Gradle 9.2.1) despite reporting success. Zip is a reliable
// substitute already used successfully by sibling modules (Varyon-Damage_Number, Varyon-TravelingCamera).
val modJar = tasks.register<Zip>("modJar") {
    group = "build"
    description = "Assemble le JAR du mod"
    archiveBaseName.set("varyon-UI")
    archiveVersion.set(version.toString())
    archiveExtension.set("jar")
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.contains("toml4j") || it.name.contains("gson") }
            .map { zipTree(it) }
    })
}

tasks.named("assemble") {
    dependsOn(modJar)
}

val exportModJar = tasks.register<Copy>("exportModJar") {
    group = "build"
    description = "Copie le JAR vers Varyon-Monorepo/build/output"
    dependsOn(modJar)
    from(modJar)
    into(rootProject.layout.buildDirectory.dir("output"))
}

tasks.named("build") {
    dependsOn(modJar)
    finalizedBy(exportModJar)
}

afterEvaluate {
    val targetTask = tasks.findByName("runServer") ?: tasks.findByName("server")
    if (targetTask != null) {
        targetTask.finalizedBy(tasks.register<Copy>("syncAssets") {
            group = "hytale"
            from(layout.buildDirectory.dir("resources/main"))
            into("src/main/resources")
            exclude("manifest.json")
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        })
        logger.lifecycle("✅ specific task '${targetTask.name}' hooked for auto-sync.")
    } else {
        logger.warn("⚠️ Could not find 'runServer' or 'server' task to hook auto-sync into.")
    }
}