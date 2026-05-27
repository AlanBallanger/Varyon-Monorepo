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
    compileOnly("org.jetbrains:annotations:26.0.2-1")
    compileOnly("org.jspecify:jspecify:1.0.0")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    compileOnly("com.google.code.gson:gson:2.11.0")
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

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveBaseName.set("Varyon-Progression")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:-removal")
}

tasks.named<JavaCompile>("compileJava") {
    doFirst {
        val f = resolvedHytaleServerJar
        if (!f.isFile || f.length() < 1_000_000L) {
            throw GradleException(
                "Aucun HytaleServer.jar valide trouvé.\n" +
                    "Cherché (dans l'ordre) : variable HYTALE_SERVER_JAR, puis :\n" +
                    "  - ${file("libs/HytaleServer.jar").absolutePath}\n" +
                    "  - ${file("../Varyon-Comet/libs/HytaleServer.jar").absolutePath}\n" +
                    "  - ${file("../Varyon/libs/HytaleServer.jar").absolutePath}\n" +
                    "Copie le JAR serveur dans l'un de ces emplacements (souvent > 50 Mo)."
            )
        }
    }
}
