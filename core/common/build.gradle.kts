import mihon.gradle.extensions.compileDesktopCodeAgainstAndroidApi
import mihon.gradle.extensions.desktop
import mihon.gradle.extensions.keepKotlinModuleName
import mihon.gradle.extensions.skipDesktopPublication
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.spotless)

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
        namespace = "eu.kanade.tachiyomi.core.common"
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
        api(libs.logcat)

        api(libs.rxJava)

        api(libs.okhttp.core)
        api(libs.okhttp.logging)
        api(libs.okhttp.brotli)
        api(libs.okhttp.dnsOverHttps)
        api(libs.okio)

        api(libs.kotlinx.coroutines.core)
        api(libs.kotlinx.serialization.json)
        api(libs.kotlinx.serialization.jsonOkio)

        implementation(libs.jsoup)

        // Sort
        implementation(libs.natural.comparator)
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation(libs.bundles.test)
                implementation(libs.okhttp.mockwebserver3)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.image.decoder)

                implementation(libs.unifile)
                implementation(libs.archive)

                api(libs.androidx.preference)
                implementation(libs.androidx.webkit)
            }
        }
    }

    keepKotlinModuleName("common")

    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
            "-Xexpect-actual-classes",
        )
    }
}

// The Entry SDK has always published this module as core-common; the multiplatform publications are named after the
// project (common, common-android, common-desktop) unless renamed.
publishing {
    publications.withType<MavenPublication>().configureEach {
        artifactId = "core-$artifactId"
    }
}

skipDesktopPublication()

// The desktop jar keeps every member extensions link against with its Android signature, for the desktop extension
// host to run them on its Android stand-ins
compileDesktopCodeAgainstAndroidApi(libs.unifile)
