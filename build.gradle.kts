import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

// IntelliJ Platform 2024.3 only runs on JDK 17/21. Without this, Gradle (and runIde) falls back
// to whatever JDK is on PATH; on newer machines (JDK 25+) that crashes the sandbox IDE at startup
// because its bundled Gradle plugin can't parse a JavaVersion it predates.
kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    pluginConfiguration {
        name = properties("pluginName")
        version = properties("pluginVersion")

        // Extract the <!-- Plugin description --> section from README.md for the marketplace listing,
        // so the feature list only needs to be maintained in one place.
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }

    signing {
        certificateChain = environment("CERTIFICATE_CHAIN")
        privateKey = environment("PRIVATE_KEY")
        password = environment("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = environment("PUBLISH_TOKEN")
    }
}

dependencies {
    // Bundled standalone SQL DDL parser so SQL mode works on every IDE
    // (including Community editions, which have no Database Tools / SQL plugin).
    implementation(libs.jsqlparser)

    testImplementation(libs.junit)

    intellijPlatform {
        intellijIdea("2024.3")
        compatiblePlugin("PythonCore")
        pluginVerifier()
        testFramework(TestFrameworkType.Platform)
    }
}

// ---------------------------------------------------------------------------
// Tooling: dump the column-type registry to JSON for the docs scraper.
//
//   ./gradlew dumpTypeManifest   →  build/types-manifest.json
//
// The Python script under scripts/ reads this file and fetches the per-type docs HTML.
// ---------------------------------------------------------------------------
tasks.register<JavaExec>("dumpTypeManifest") {
    group = "documentation"
    description = "Dump the ColumnTypes registry as JSON for the docs-scraper Python script."
    dependsOn("classes")
    // gradle.properties sets kotlin.stdlib.default.dependency=false so the IDE bundle provides
    // kotlin-stdlib at runtime in the plugin context. For a standalone JavaExec we have to add
    // it back explicitly, otherwise the JVM fails to load NoWhenBranchMatchedException et al.
    // Hardcoded to match `org.jetbrains.kotlin.jvm` plugin version pinned in settings.gradle.kts.
    classpath = sourceSets["main"].runtimeClasspath +
        configurations.detachedConfiguration(
            dependencies.create("org.jetbrains.kotlin:kotlin-stdlib:2.2.20")
        )
    mainClass.set("com.noisebomb.sqlalchemy.tools.DumpTypeManifestKt")
    val outputFile = layout.buildDirectory.file("types-manifest.json")
    outputs.file(outputFile)
    doFirst {
        val f = outputFile.get().asFile
        f.parentFile.mkdirs()
        standardOutput = f.outputStream()
    }
}
