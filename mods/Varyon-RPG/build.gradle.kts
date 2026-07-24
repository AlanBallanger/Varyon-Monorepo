import java.util.zip.ZipFile

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

    implementation("org.xerial:sqlite-jdbc:3.46.1.3")
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

val sqliteNativePlatform = findProperty("sqlite_native_platform")?.toString()?.trim('/') ?: "Linux/x86_64"
val sqliteNativePrefix = "org/sqlite/native/$sqliteNativePlatform/"

val fatJar = tasks.register<Jar>("fatJar") {
    archiveClassifier.set("")
    archiveBaseName.set("Varyon-RPG")
    archiveVersion.set(version.toString())
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output)

    val implementationJars = configurations.runtimeClasspath.get()
        .filter { it.name.contains("sqlite-jdbc") }
    from(implementationJars.map { zipTree(it) }) {
        eachFile {
            val path = relativePath.pathString.replace('\\', '/')
            if (path.startsWith("org/sqlite/native/") && !path.startsWith(sqliteNativePrefix)) {
                exclude()
            }
        }
    }

    exclude("META-INF/versions/**")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    exclude("META-INF/*.SF")
    exclude("module-info.class")
    exclude("**/package-info.class")

    manifest {
        attributes["Specification-Title"] = rootProject.name
        attributes["Specification-Version"] = version
        attributes["Implementation-Title"] = project.name
        attributes["Implementation-Version"] = version.toString()
    }
}

val verifyModJar = tasks.register("verifyModJar") {
    dependsOn(fatJar)
    doLast {
        val jarFile = fatJar.get().archiveFile.get().asFile
        require(jarFile.isFile) { "Missing mod JAR: ${jarFile.absolutePath}" }
        require(jarFile.length() > 100_000L) {
            "Mod JAR looks truncated (${jarFile.length()} bytes): ${jarFile.name}"
        }

        ZipFile(jarFile).use { zip ->
            val entry = zip.getEntry("manifest.json")
                ?: error("manifest.json missing from ${jarFile.name}")
            val manifest = zip.getInputStream(entry).bufferedReader().use { it.readText() }
            require(!manifest.contains("\${")) {
                "manifest.json still contains Gradle placeholders in ${jarFile.name}"
            }
            require(manifest.contains("\"Main\"") && manifest.contains("VaryonRpgPlugin")) {
                "manifest.json is invalid in ${jarFile.name}"
            }

            val nativeEntries = zip.entries().asSequence()
                .map { it.name }
                .filter { it.startsWith("org/sqlite/native/") && !it.endsWith("/") }
                .toList()
            require(nativeEntries.isNotEmpty()) {
                "No sqlite native library found for platform $sqliteNativePlatform in ${jarFile.name}"
            }
            require(nativeEntries.all { it.startsWith(sqliteNativePrefix) }) {
                "Unexpected sqlite native entries in ${jarFile.name}: ${nativeEntries.joinToString()}"
            }
        }
    }
}

val exportModJar = tasks.register<Copy>("exportModJar") {
    group = "build"
    description = "Copie le JAR vers Varyon-Monorepo/build/output"
    dependsOn(fatJar, verifyModJar)
    from(fatJar)
    into(rootProject.layout.buildDirectory.dir("output"))
}

tasks.named("build") {
    dependsOn(fatJar)
    finalizedBy(verifyModJar, exportModJar)
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

