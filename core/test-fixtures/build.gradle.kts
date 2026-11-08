import app.katari.gradle.extensions.compileSharedCodeAgainstAndroidApi
import app.katari.gradle.extensions.desktop

// Test doubles shared by more than one module's tests. Only test source sets depend on this module.
plugins {
    alias(katari.plugins.kotlin.multiplatform)
    alias(katari.plugins.spotless)
}

kotlin {
    android {
        namespace = "app.katari.core.testfixtures"
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.core.common)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}

// FakeSharedPreferences stands in for the Android SharedPreferences the extension host hands extensions
compileSharedCodeAgainstAndroidApi()
