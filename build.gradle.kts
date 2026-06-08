import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

group = "com.noisebomb.sqlalchemy"
version = "0.0.1"

dependencies {
    testImplementation(libs.junit)

    intellijPlatform {
        intellijIdea("2025.3.5")
        compatiblePlugin("PythonCore")
        testFramework(TestFrameworkType.Platform)
    }
}
