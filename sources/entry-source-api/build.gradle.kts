import app.katari.gradle.extensions.compileSharedCodeAgainstAndroidApi
import app.katari.gradle.extensions.desktop
import app.katari.gradle.extensions.skipDesktopPublication
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(katari.plugins.kotlin.multiplatform)
    alias(katari.plugins.spotless)

    alias(libs.plugins.kotlin.serialization)
    id("maven-publish")
}

group = "com.github.pa2x2.katari"
version = providers.gradleProperty("sourceApiVersion")
    .orElse(System.getenv("VERSION") ?: "local-SNAPSHOT")
    .get()

kotlin {
    @Suppress("UnstableApiUsage")
    android {
        namespace = "eu.kanade.tachiyomi.source.entry"
        optimization {
            consumerKeepRules.apply {
                publish = true
                file("consumer-proguard.pro")
            }
        }

        // TODO(antsy): Remove when https://youtrack.jetbrains.com/issue/KT-83319 is resolved
        withHostTest { }
    }
    desktop()

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        api(projects.core.common)
        api(projects.sources.bookApi)

        api(libs.kotlinx.serialization.json)
        api(libs.kotlinx.serialization.jsonOkio)
        api(libs.injekt)
        api(libs.jsoup)

        compileOnly(platform(libs.androidx.compose.bom))
        compileOnly(libs.androidx.compose.runtimeAnnotation)
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        androidMain {
            dependencies {
                api(libs.androidx.preference)
                implementation(libs.androidx.annotation)
            }
        }
    }
}

compileSharedCodeAgainstAndroidApi()
skipDesktopPublication()
