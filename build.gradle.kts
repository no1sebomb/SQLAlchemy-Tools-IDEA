import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

group = "com.noisebomb.sqlalchemy"
version = "0.0.3"

dependencies {
    testImplementation(libs.junit)

    intellijPlatform {
        intellijIdea("2024.3")
        compatiblePlugin("PythonCore")
        testFramework(TestFrameworkType.Platform)
    }
}
