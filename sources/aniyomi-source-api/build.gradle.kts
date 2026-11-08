import app.katari.gradle.extensions.compileSharedCodeAgainstAndroidApi
import app.katari.gradle.extensions.desktop
import app.katari.gradle.extensions.keepKotlinModuleName
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(katari.plugins.kotlin.multiplatform)
    alias(katari.plugins.spotless)
}

kotlin {
    @Suppress("UnstableApiUsage")
    android {
        namespace = "app.katari.aniyomi.source"
        optimization {
            consumerKeepRules.apply {
                publish = true
                file("consumer-proguard.pro")
            }
        }
    }
    desktop()

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        api(projects.core.common)
        api(libs.injekt)
        api(libs.jsoup)
        api(libs.nanohttpd)
    }

    keepKotlinModuleName("aniyomi-source-api")
}

compileSharedCodeAgainstAndroidApi()
