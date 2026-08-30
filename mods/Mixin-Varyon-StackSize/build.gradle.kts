plugins {
    java
    idea
}

group = "fr.varyon"
version = "0.2.0"
val javaVersion = 25

repositories {
    mavenCentral()
}

dependencies {
    // API du serveur Hytale (ClassTransformer, classes ciblées par l'ASM injector).
    // Fourni par le serveur au runtime — jamais embarqué dans le jar final.
    compileOnly(files("$rootDir/libs/HytaleServer.jar"))
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

// Source de vérité versionnée et lisible (src/main/data/stackable_items.json :
// { "ItemId": { "baseStack": 100, "categories": ["wood", ...] } }). Compilée avant
// processResources vers le format compact "id|stack|cat1,cat2;..." attendu par
// MaxStackPermissionInjector (une seule constante String embarquable en bytecode —
// le code injecté dans les classes serveur ne peut utiliser qu'un parseur JDK nu,
// pas de lib JSON, cf. commentaire de l'injector).
val stackDataSource = layout.projectDirectory.file("src/main/data/stackable_items.json")
val stackDataGenDir = layout.buildDirectory.dir("generated/stackData")
val stackDataOutput = stackDataGenDir.map { it.file("fr/varyon/mixinagent/stackable_items.txt") }

val generateStackTable = tasks.register("generateStackTable") {
    inputs.file(stackDataSource)
    outputs.file(stackDataOutput)

    doLast {
        @Suppress("UNCHECKED_CAST")
        val parsed = groovy.json.JsonSlurper().parse(stackDataSource.asFile) as Map<String, Map<String, Any>>

        val encoded = parsed.entries
            .sortedBy { it.key }
            .joinToString(";") { (id, entry) ->
                val baseStack = entry["baseStack"]
                @Suppress("UNCHECKED_CAST")
                val categories = entry["categories"] as List<String>
                "$id|$baseStack|${categories.joinToString(",")}"
            }

        val outFile = stackDataOutput.get().asFile
        outFile.parentFile.mkdirs()
        outFile.writeText(encoded, Charsets.UTF_8)
    }
}

sourceSets.main {
    resources.srcDir(stackDataGenDir)
}

tasks.named("processResources") {
    dependsOn(generateStackTable)
}

val earlyPluginJar = tasks.register<Jar>("earlyPluginJar") {
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
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
