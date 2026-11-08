import mihon.gradle.extensions.desktop
import mihon.gradle.extensions.keepKotlinModuleName

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.spotless)
    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "app.katari.script.runtime"

        // TODO(antsy): Remove when https://youtrack.jetbrains.com/issue/KT-83319 is resolved
        withHostTest { }
    }
    desktop()

    keepKotlinModuleName("script-runtime")

    sourceSets {
        commonMain {
            dependencies {
                api(projects.core.common)
                implementation(libs.okhttp.zstd)

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
