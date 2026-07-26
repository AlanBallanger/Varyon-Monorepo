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
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveBaseName.set("Varyon_Comet")
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
