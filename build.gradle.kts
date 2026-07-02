plugins {
    kotlin("jvm") version "2.3.20"
    id("com.gradleup.shadow") version "8.3.6"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "io.github.darkstarworks"

// Two build targets from one source, selected with -Pmc=<line> (default 26):
//   ./gradlew shadowJar -Pmc=21   ->  PluginGuard-1.2.0.jar        (compile 1.21.x, Java 21)
//   ./gradlew shadowJar -Pmc=26   ->  PluginGuard-1.2.0-mc26.jar   (compile 26.x,  Java 25)
// 1.21.x servers run JDK21 and can't load Java 25 bytecode, hence the two artifacts.
val pluginVersion = "1.2.0"
val mcLine = (findProperty("mc") as String?) ?: "26"
val is26 = mcLine == "26"
version = if (is26) "$pluginVersion-mc26" else pluginVersion

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}

dependencies {
    compileOnly(
        if (is26) "io.papermc.paper:paper-api:26.1.2.build.66-stable"
        else "io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT"
    )
    // Netty types for the in-game brand spoofer's pipeline handler. compileOnly — the server ships
    // Netty at runtime, so nothing is added to the jar. All server internals are reached via
    // reflection, so no paperweight/dev-bundle is needed and this still builds on plain paper-api.
    compileOnly("io.netty:netty-transport:4.1.101.Final")
    compileOnly("io.netty:netty-common:4.1.101.Final")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

// 1.21.x servers run JDK21 and cannot load newer bytecode; 26.x runs JDK25 and loads either.
// So the 1.21 artifact must be Java 21; the 26 artifact targets Java 25.
val targetJavaVersion = if (is26) 25 else 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks {
    runServer {
        minecraftVersion(if (is26) "26.1.2" else "1.21.8")
    }
    build {
        dependsOn("shadowJar")
    }
    jar {
        // The plain (non-shaded) jar has no runtime use - skip building it entirely.
        enabled = false
    }
    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
    shadowJar {
        archiveClassifier.set("")
    }
}
