plugins {
    java
    idea
}

group = "fr.varyon"
version = "0.1.0"
val javaVersion = 25

repositories {
    mavenCentral()
}

dependencies {
    // API du serveur Hytale (ClassTransformer, classes ciblées par l'ASM injector).
    // Fourni par le serveur au runtime — jamais embarqué dans le jar final.
    compileOnly(files("$rootDir/../../../libs/HytaleServer.jar"))
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    // ASM brut (Tree API) : bien plus léger que ByteBuddy pour un early-plugin.
    // 9.8+ requis pour lire des class files compilés en Java 25 (major version 69).
    implementation("org.ow2.asm:asm:9.8")
    implementation("org.ow2.asm:asm-tree:9.8")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
}

val earlyPluginJar = tasks.register<Jar>("earlyPluginJar") {
    archiveClassifier.set("")
    archiveBaseName.set("Mixin-Varyon-StackSize")
    archiveVersion.set(version.toString())
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output)
    from({ configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) } })
}

tasks.named("build") {
    dependsOn(earlyPluginJar)
}
