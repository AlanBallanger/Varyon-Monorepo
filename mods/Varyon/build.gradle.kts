import org.gradle.api.tasks.bundling.Zip

plugins {
    `maven-publish`
    idea
    id("hytale-mod") version "0.+"
}

group = "com.example"
version = "0.1.4"
val javaVersion = 25

repositories {
    mavenCentral()
    maven("https://maven.hytale-modding.info/releases") {
        name = "HytaleModdingReleases"
    }
    maven("https://maven.hytale-modding.info/pre-release") {
        name = "HytaleModdingPreRelease"
    }
    maven("https://oss.sonatype.org/content/repositories/snapshots/") {
        name = "SonatypeSnapshots"
    }
    maven("https://repo.codemc.io/repository/creatorfromhell/") {
        name = "VaultUnlocked"
    }
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.1.0")
    compileOnly("org.jspecify:jspecify:0.3.0")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("net.cfh.vault:VaultUnlocked:2.18.3")
    compileOnly(files("libs/NameplateBuilder-API-1.0.0.jar"))
    implementation("com.moandjiezana.toml:toml4j:0.7.2")
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("org.slf4j:slf4j-simple:2.0.9")
}

// Standard Jar task was found to intermittently/consistently fail to write its output file in
// this environment (Java 25 + Gradle 9.2.1) despite reporting success. Zip is a reliable
// substitute already used successfully by sibling modules (Varyon-Damage_Number, Varyon-TravelingCamera).
val fatJar = tasks.register<Zip>("fatJar") {
    archiveBaseName.set("Varyon")
    archiveVersion.set("0.1.4")
    archiveExtension.set("jar")
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output)

    // Inclure seulement les dépendances implementation (toml4j, sqlite-jdbc, slf4j)
    val implementationJars = configurations.runtimeClasspath.get()
        .filter { it.name.contains("toml4j") || it.name.contains("sqlite-jdbc") || it.name.contains("slf4j") }

    from({
        implementationJars.map { zipTree(it) }
    })

    // sqlite-jdbc bundles native libs for every OS/arch; the server only runs on Linux x86_64.
    exclude { it.path.startsWith("org/sqlite/native/") && !it.path.startsWith("org/sqlite/native/Linux/x86_64/") }
}

// Le plugin hytale-mod force destinationDirectory des taches d'archive vers build/output a la
// racine : fatJar ecrit donc deja directement a destination. L'ancienne tache Copy recopiait le
// fichier sur lui-meme, ce qui le tronquait a 0 octet. On se contente de verifier le resultat.
val exportModJar = tasks.register("exportModJar") {
    group = "build"
    description = "Verifie le JAR exporte vers Varyon-Monorepo/build/output"
    dependsOn(fatJar)
    val archive = fatJar.flatMap { it.archiveFile }
    doLast {
        val jar = archive.get().asFile
        if (!jar.isFile || jar.length() == 0L) {
            throw GradleException("Export du mod echoue : ${jar.path} est absent ou vide")
        }
        logger.lifecycle("Mod exporte : ${jar.path} (${jar.length()} octets)")
    }
}

tasks.named("build") {
    dependsOn(fatJar)
    finalizedBy(exportModJar)
}

hytale {
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }

    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.named<ProcessResources>("processResources") {
    var replaceProperties = mapOf(
        "plugin_group" to findProperty("plugin_group"),
        "plugin_maven_group" to project.group,
        "plugin_name" to project.name,
        "plugin_version" to project.version,
        "server_version" to findProperty("server_version"),

        "plugin_description" to findProperty("plugin_description"),
        "plugin_website" to findProperty("plugin_website"),

        "plugin_main_entrypoint" to findProperty("plugin_main_entrypoint"),
        "plugin_author" to findProperty("plugin_author")
    )

    filesMatching("manifest.json") {
        expand(replaceProperties)
    }

    inputs.properties(replaceProperties)
}

tasks.withType<Jar> {
    manifest {
        attributes["Specification-Title"] = rootProject.name
        attributes["Specification-Version"] = version
        attributes["Implementation-Title"] = project.name
        attributes["Implementation-Version"] =
            providers.environmentVariable("COMMIT_SHA_SHORT")
                .map { "${version}-${it}" }
                .getOrElse(version.toString())
    }
}

publishing {
    repositories {
        // This is where you put repositories that you want to publish to.
        // Do NOT put repositories for your dependencies here.
    }

    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

// IDEA no longer automatically downloads sources/javadoc jars for dependencies, so we need to explicitly enable the behavior.
idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

val syncAssets = tasks.register<Copy>("syncAssets") {
    group = "hytale"
    description = "Automatically syncs assets from Build back to Source after server stops."

    // Take from the temporary build folder (Where the game saved changes)
    from(layout.buildDirectory.dir("resources/main"))

    // Copy into your actual project source (Where your code lives)
    into("src/main/resources")

    // IMPORTANT: Protect the manifest template from being overwritten
    exclude("manifest.json")

    // If a file exists, overwrite it with the new version from the game
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    doLast {
        println("✅ Assets successfully synced from Game to Source Code!")
    }
}

afterEvaluate {
    // Now Gradle will find it, because the plugin has finished working
    val targetTask = tasks.findByName("runServer") ?: tasks.findByName("server")

    if (targetTask != null) {
        targetTask.finalizedBy(syncAssets)
        logger.lifecycle("✅ specific task '${targetTask.name}' hooked for auto-sync.")
    } else {
        logger.warn("⚠️ Could not find 'runServer' or 'server' task to hook auto-sync into.")
    }
}
