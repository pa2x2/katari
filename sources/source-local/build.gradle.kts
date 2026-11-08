import app.katari.gradle.extensions.desktop
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(katari.plugins.kotlin.multiplatform)
    alias(katari.plugins.spotless)
    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "tachiyomi.source.local"

        // TODO(antsy): Remove when https://youtrack.jetbrains.com/issue/KT-83319 is resolved
        withHostTest { }
    }
    desktop()

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        api(projects.sources.entrySourceApi)
        api(projects.core.i18n)
        api(projects.core.archive)
        api(projects.core.common)

        implementation(projects.core.coreMetadata)

        // Move ChapterRecognition to separate module?
        implementation(projects.core.domain)

        implementation(libs.kotlinx.serialization.json)
        implementation(libs.xmlutil.serialization)
        implementation(libs.jsoup)
    }

    sourceSets {
        androidMain {
            dependencies {
                implementation(libs.xmlutil.core)
            }
        }
        getByName("desktopMain") {
            dependencies {
                implementation(libs.xmlutil.core.multiplatform)
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlinx.serialization.ExperimentalSerializationApi")
    }
}
