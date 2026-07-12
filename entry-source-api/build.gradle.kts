import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.spotless)

    alias(libs.plugins.kotlin.serialization)
    id("maven-publish")
}

group = "com.github.katariapp.katari"
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

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        api(projects.core.common)

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

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
