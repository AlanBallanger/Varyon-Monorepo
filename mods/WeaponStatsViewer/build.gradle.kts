import org.gradle.api.tasks.bundling.Zip

plugins {
    java
}

group = "com.woxtz.weaponinfo"
version = "1.7.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:26.0.2-1")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    compileOnly(files("libs/HytaleServer.jar"))
    compileOnly("com.google.code.gson:gson:2.11.0")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

sourceSets {
    named("main") {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("resources"))
    }
}

tasks.named<Jar>("jar") {
    enabled = false
}

// Standard Jar task unreliably fails to write output in this environment
// (Java 25 + Gradle 9.2.1); Zip is used instead, matching the Varyon-Monorepo mods.
val modJar = tasks.register<Zip>("modJar") {
    group = "build"
    description = "Assemble le JAR du mod (avec dépendances embarquées)"
    archiveBaseName.set("WeaponStatsViewer")
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
