import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.10"
    application
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

task("runIntegralityGapUI", JavaExec::class) {
    main = "nl.tue.co.rnd.ui.IntegralityGapUI"
    classpath = sourceSets["main"].runtimeClasspath
}

task("runCapHoseIntegralityGapUI", JavaExec::class) {
    main = "nl.tue.co.rnd.ui.CapHoseIntegralityGapUI"
    classpath = sourceSets["main"].runtimeClasspath
}

task("jarRnd", Jar::class) {
    archiveBaseName.set("rnd")
    version = null

    from(configurations.runtimeClasspath.get().map({ if (it.isDirectory) it else zipTree(it) })) {
        exclude("gurobi/*")
    }
    with(tasks.jar.get() as CopySpec)
}
