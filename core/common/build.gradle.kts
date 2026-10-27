import mihon.gradle.extensions.desktop
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

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
        getByName("androidHostTest") {
            dependencies {
                implementation(libs.okhttp.mockwebserver3)
            }
        }
    }

    // Internal members compile to public JVM methods suffixed with the module name, and published extensions link
    // against them (pinned by verifyCoreCommonAbi30). The Android library build named the module `common`; desktop keeps
    // the name so converted extensions link there too.
    targets.configureEach {
        compilations.named { it == KotlinCompilation.MAIN_COMPILATION_NAME }.configureEach {
            compileTaskProvider.configure {
                (compilerOptions as? KotlinJvmCompilerOptions)?.moduleName?.set("common")
            }
        }
    }

    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
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
