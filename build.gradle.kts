plugins {
    kotlin("jvm") version "2.4.20"
    id("com.gradleup.shadow") version "9.6.1"
    // 3.x uses Paper's current downloads API; 2.3.1 used the retired v2 API.
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = "io.github.darkstarworks"

// Three build targets from one source, selected with -Pmc=<line> (default 26):
//   ./gradlew shadowJar -Pmc=21   ->  PluginGuard-1.4.0.jar         (1.21.x,       Java 21)
//   ./gradlew shadowJar -Pmc=26   ->  PluginGuard-1.4.0-mc26.jar    (26.1 to 26.2, Java 25)
//   ./gradlew shadowJar -Pmc=263  ->  PluginGuard-1.4.0-mc263.jar   (26.3,         Java 25)
// 1.21.x servers run JDK21 and can't load Java 25 bytecode. The 26.3 jar declares
// api-version 26.3, so an older server refuses it, and it follows its own update track.
val pluginVersion = "1.4.0"
val mcLine = (findProperty("mc") as String?) ?: "26"

data class McTarget(val paperApi: String, val suffix: String, val apiVersion: String, val java: Int, val runMc: String)

val mcTarget = when (mcLine) {
    "21" -> McTarget("1.21.11-R0.1-SNAPSHOT", "", "1.21", 21, "1.21.11")
    "26" -> McTarget("26.2.build.129-stable", "-mc26", "1.21", 25, "26.2")
    "263" -> McTarget("26.3.build.49-alpha", "-mc263", "26.3", 25, "26.3")
    else -> throw GradleException("Unknown -Pmc=$mcLine. Use 21, 26 or 263.")
}
version = "$pluginVersion${mcTarget.suffix}"
// PluginPulse only applies a track on 26.x servers, so the 1.21 jar's value is never read.
val updateTrack = if (mcLine == "263") "mc263" else "mc26"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    // Each extra repository may only serve its own packages, so nothing else can be swapped in.
    maven("https://jitpack.io") {
        name = "jitpack"
        content { includeGroup("com.github.ESMP-FUN.PluginPulse") }
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${mcTarget.paperApi}")
    // Netty types for the brand spoofer's pipeline handler. compileOnly: the server ships Netty
    // (4.1 on 1.21.x, 4.2 on 26.x) and only API common to both is used. Server internals are
    // reached via reflection, so no paperweight/dev-bundle is needed.
    compileOnly("io.netty:netty-transport:4.1.101.Final")
    compileOnly("io.netty:netty-common:4.1.101.Final")
    implementation(kotlin("stdlib"))
    // PluginPulse: update checks and checksum-verified downloads. Relocated below.
    implementation("com.github.ESMP-FUN.PluginPulse:pluginpulse-core:v0.9.0")
}

kotlin {
    jvmToolchain(mcTarget.java)
}

tasks {
    runServer {
        // Overridable so one jar can be smoke-tested across its line, e.g. -PrunMc=26.1.2.
        minecraftVersion((findProperty("runMc") as String?) ?: mcTarget.runMc)
        jvmArgs("-Xms256M", "-Xmx1G")
    }
    build {
        dependsOn("shadowJar")
    }
    jar {
        // The plain (non-shaded) jar has no runtime use - skip building it entirely.
        enabled = false
    }
    processResources {
        val props = mapOf("version" to version, "apiVersion" to mcTarget.apiVersion, "track" to updateTrack)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching(listOf("plugin.yml", "pluginpulse.yml")) {
            expand(props)
        }
    }
    shadowJar {
        archiveClassifier.set("")
        // Relocate PluginPulse so it can't clash with another plugin's shaded copy.
        relocate("io.github.darkstarworks.pluginpulse", "io.github.darkstarworks.pluginguard.pluginpulse")
        // Drops the parts of the Kotlin library the plugin never calls. PluginPulse stays whole
        // because it finds some of its own classes by name.
        minimize {
            exclude(dependency("com.github.ESMP-FUN.PluginPulse:.*:.*"))
        }
        exclude("META-INF/maven/**")
        exclude("META-INF/versions/*/module-info.class")
        exclude("module-info.class")
    }
}
