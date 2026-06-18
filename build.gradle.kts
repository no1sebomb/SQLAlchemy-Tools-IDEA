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

//    signing {
//        certificateChain = environment("CERTIFICATE_CHAIN")
//        privateKey = environment("PRIVATE_KEY")
//        password = environment("PRIVATE_KEY_PASSWORD")
//    }
//
//    publishing {
//        token = environment("PUBLISH_TOKEN")
//    }
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
