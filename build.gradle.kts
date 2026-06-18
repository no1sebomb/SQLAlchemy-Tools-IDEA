import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

group = "com.noisebomb.sqlalchemy"
version = "0.0.3"

dependencies {
    // Bundled standalone SQL DDL parser so SQL mode works on every IDE
    // (including Community editions, which have no Database Tools / SQL plugin).
    implementation(libs.jsqlparser)

    testImplementation(libs.junit)

    intellijPlatform {
        intellijIdea("2024.3")
        compatiblePlugin("PythonCore")
        testFramework(TestFrameworkType.Platform)
    }
}
