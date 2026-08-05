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
    compileOnly("org.jetbrains:annotations:26.0.2-1")
    compileOnly("org.jspecify:jspecify:1.0.0")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    compileOnly(files("libs/HytaleServer.jar"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

sourceSets {
    named("main") {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("src/main/resources"))
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
    archiveBaseName.set("Varyon-Comet")
    archiveVersion.set(version.toString())
    archiveExtension.set("jar")
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
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
