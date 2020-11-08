import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.10"
}

group = "nl.tue.co"
version = "0.1.0-SNAPSHOT"

val gurobiLibDir: String? by project

repositories {
    mavenCentral()
}

dependencies {
    if (gurobiLibDir != null) {
        implementation(fileTree(gurobiLibDir!!).matching { include("gurobi.jar") })
    }

    implementation("org.graphstream:gs-core:2.0")
    implementation("org.graphstream:gs-ui-swing:2.0")

    testImplementation(kotlin("test-junit5"))
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}
