plugins {
    idea
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.3"
}

val outputDir = rootProject.layout.buildDirectory.dir("output")
