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
        file("../../libs/HytaleServer.jar"),
    ).filterNotNull()
        .map { it.normalize() }
        .firstOrNull { it.isFile && it.length() > 1_000_000L }
        ?: file("libs/HytaleServer.jar")

dependencies {
    compileOnly("org.jetbrains:annotations:26.0.2-1")
    compileOnly("org.jspecify:jspecify:1.0.0")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    compileOnly(files(resolvedHytaleServerJar))
    // Varyon est optionnel a l'execution : compileOnly permet de lire MobScalingComponent
    // (niveau des creatures) sans imposer sa presence sur les mondes qui ne le chargent pas.
    compileOnly(project(":mods:Varyon"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

// Les textes affiches au joueur sont accentues : sans encodage explicite, javac utilise
// la page de code de la plateforme (Windows-1252 ici) et les accents ressortent corrompus.
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.named<ProcessResources>("processResources") {
    filesMatching("manifest.json") { expand(project.properties) }
}

tasks.named<Jar>("jar") {
    enabled = false
}

// Meme contournement que les modules voisins : la tache Jar standard echoue par
// intermittence a ecrire sa sortie (Java 25 + Gradle 9.2.1) tout en signalant un succes.
val modJar = tasks.register<Zip>("modJar") {
    group = "build"
    description = "Assemble le JAR du mod"
    archiveBaseName.set("Varyon-Death")
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
