plugins {
    java
}

version = "4.0.0"

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
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

sourceSets {
    named("main") {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("src/main/resources"))
    }
}

tasks.named<ProcessResources>("processResources") {
    from(projectDir) {
        include("manifest.json")
    }
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveBaseName.set("Varyon_Comet")
}
