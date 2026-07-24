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
    archiveBaseName.set("varyon-UI")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    doFirst {
        from(configurations.runtimeClasspath.get()
            .filter { it.name.contains("toml4j") || it.name.contains("gson") }
            .map { zipTree(it) })
    }
}

val exportModJar = tasks.register<Copy>("exportModJar") {
    group = "build"
    description = "Copie le JAR vers Varyon-Monorepo/build/output"
    dependsOn(tasks.named("jar"))
    from(tasks.named<Jar>("jar").flatMap { it.archiveFile })
    into(rootProject.layout.buildDirectory.dir("output"))
}

tasks.named("build") {
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