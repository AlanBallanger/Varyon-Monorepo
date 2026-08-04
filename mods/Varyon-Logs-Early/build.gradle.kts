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

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveBaseName.set("Varyon-Logs-Early")
}

val exportModJar = tasks.register<Copy>("exportModJar") {
    group = "build"
    description = "Copie le JAR vers Varyon-Monorepo/build/output"
    dependsOn(tasks.named("jar"))
    val jarFileProvider = tasks.named<Jar>("jar").flatMap { it.archiveFile }
    val outputDir = rootProject.layout.buildDirectory.dir("output")
    onlyIf {
        val jarFile = jarFileProvider.get().asFile
        val destFile = outputDir.get().asFile.resolve(jarFile.name)
        jarFile.normalize() != destFile.normalize()
    }
    from(jarFileProvider)
    into(outputDir)
}

tasks.named("build") {
    finalizedBy(exportModJar)
}
