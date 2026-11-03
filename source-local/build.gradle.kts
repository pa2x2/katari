import mihon.gradle.extensions.desktop
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.spotless)
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
        api(projects.entrySourceApi)
        api(projects.i18n)
        api(projects.core.archive)
        api(projects.core.common)

        implementation(projects.coreMetadata)

        // Move ChapterRecognition to separate module?
        implementation(projects.domain)

        implementation(libs.kotlinx.serialization.json)
        implementation(libs.xmlutil.serialization)
        implementation(libs.jsoup)
    }

    sourceSets {
        androidMain {
            dependencies {
                implementation(libs.unifile)
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
