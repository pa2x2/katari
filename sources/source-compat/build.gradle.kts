import app.katari.gradle.extensions.compileSharedCodeAgainstAndroidApi
import app.katari.gradle.extensions.desktop
import app.katari.gradle.extensions.keepKotlinModuleName

plugins {
    alias(katari.plugins.kotlin.multiplatform)
    alias(katari.plugins.spotless)
    alias(libs.plugins.metro)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "eu.kanade.tachiyomi.source.compat"

        // TODO(antsy): Remove when https://youtrack.jetbrains.com/issue/KT-83319 is resolved
        withHostTest { }
    }
    desktop()

    keepKotlinModuleName("source-compat")

    sourceSets {
        commonMain {
            dependencies {
                api(projects.sources.entrySourceApi)
                api(projects.sources.sourceApi)
                implementation(projects.sources.aniyomiSourceApi)
                implementation(projects.core.platform)
                // The source settings host reads a source's settings through the entry interactions' settings feature.
                implementation(projects.features.entryInteractions.api)
                // The host's projections of source filters, which the legacy and Aniyomi filter bindings unwrap.
                implementation(projects.core.domain)
                implementation(projects.sources.scriptRuntime)
                implementation(libs.kotlinx.serialization.json)

                // quickjs-kt is released as one artifact per platform without Gradle metadata, the Android AAR and the
                // JVM jar with the same API, so shared code compiles against the JVM jar and each platform runs its own
                compileOnly(libs.quickjs.kt.jvm)
            }
        }
        getByName("desktopMain") {
            dependencies {
                implementation(libs.quickjs.kt.jvm)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.bundles.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.okhttp.mockwebserver3)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }
        getByName("desktopTest") {
            dependencies {
                // The Android classes the plugins' settings and storage are handed, as the extension host gives them
                runtimeOnly(projects.sources.extensionRuntime.androidCompat)
            }
        }
    }
}

dependencies {
    // Named as an AAR, which Gradle can't tell without metadata
    "androidMainImplementation"(libs.quickjs.kt.android) {
        artifact { type = "aar" }
    }
}

// Host tests can't load the AAR's Android libraries; the JVM jar carries the same engine for desktop platforms.
configurations.matching { it.name == "androidHostTestRuntimeClasspath" }.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module(libs.quickjs.kt.android.get().module.toString()))
            .using(module(libs.quickjs.kt.jvm.get().toString()))
            .withoutArtifactSelectors()
    }
}

// LNReader plugins' settings and storage and the Aniyomi bridges hand extensions SharedPreferences and preference screens,
// and the source settings host reads those screens back
compileSharedCodeAgainstAndroidApi()
