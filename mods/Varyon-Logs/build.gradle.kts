plugins {
    java
}

group = properties["plugin_group"] as String
version = properties["plugin_version"] as String

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(files("libs/HytaleServer.jar"))
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
    archiveBaseName.set("Varyon-Logs")
}
