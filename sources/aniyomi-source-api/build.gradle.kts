import mihon.gradle.extensions.compileSharedCodeAgainstAndroidApi
import mihon.gradle.extensions.desktop
import mihon.gradle.extensions.keepKotlinModuleName
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.spotless)
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
