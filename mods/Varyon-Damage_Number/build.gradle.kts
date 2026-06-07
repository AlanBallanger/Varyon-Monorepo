import org.gradle.api.tasks.bundling.Zip

plugins {
    java
}

group = properties["plugin_group"] as String
version = properties["plugin_version"] as String

repositories {
    mavenCentral()
}

val resolvedHytaleServerJar =
    sequenceOf(
        System.getenv("HYTALE_SERVER_JAR")?.trim()?.takeIf { it.isNotEmpty() }?.let { file(it) },
        file("libs/HytaleServer.jar"),
        file("../Varyon-Comet/libs/HytaleServer.jar"),
        file("../Varyon/libs/HytaleServer.jar"),
    ).filterNotNull()
        .map { it.normalize() }
        .firstOrNull { it.isFile && it.length() > 1_000_000L }
        ?: file("libs/HytaleServer.jar")

dependencies {
    compileOnly(files(resolvedHytaleServerJar))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.named<ProcessResources>("processResources") {
    filesMatching("manifest.json") { expand(project.properties) }
}

val modJar = tasks.register<Zip>("modJar") {
    group = "build"
    description = "Assemble le JAR du mod"
    archiveBaseName.set("Varyon-Damage_Number")
    archiveVersion.set(version.toString())
    archiveExtension.set("jar")
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
}

tasks.named<Jar>("jar") {
    enabled = false
}

val exportModJar = tasks.register<Copy>("exportModJar") {
    group = "build"
    description = "Copie le JAR vers Varyon-Monorepo/build/output"
    dependsOn(modJar)
    from(modJar)
    into(layout.projectDirectory.dir("../../build/output"))
}

tasks.named("assemble") {
    dependsOn(modJar)
}

tasks.named("build") {
    dependsOn(exportModJar)
}

tasks.named<JavaCompile>("compileJava") {
    doFirst {
        val serverJar = resolvedHytaleServerJar
        if (!serverJar.isFile || serverJar.length() < 1_000_000L) {
            throw GradleException(
                "Aucun HytaleServer.jar valide trouvé pour Varyon-Damage_Number.\n" +
                    "Copie le JAR serveur dans mods/Varyon-Damage_Number/libs/HytaleServer.jar"
            )
        }
    }
}
