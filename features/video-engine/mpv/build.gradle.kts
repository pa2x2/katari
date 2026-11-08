import mihon.gradle.extensions.desktop
import mihon.gradle.tasks.UnpackAarGlueTask

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.spotless)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

// The libmpv AAR's native libraries belong to the separately installed mpv add-on, so only its glue is used here.
val libmpvAar = configurations.register("libmpvAar") {
    isCanBeConsumed = false
    isTransitive = false
}

val unpackLibmpvGlue = tasks.register<UnpackAarGlueTask>("unpackLibmpvGlue") {
    aar.from(libmpvAar)
    classesJar.set(layout.buildDirectory.file("libmpv/classes.jar"))
    assetsDir.set(layout.buildDirectory.dir("libmpv/assets"))
}

kotlin {
    @Suppress("UnstableApiUsage")
    android {
        namespace = "mihon.video.engine.mpv"
        // The glue's assets, which name the add-on's libraries and the glue level it must have.
        androidResources {
            enable = true
        }
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

    sourceSets {
        commonMain {
            dependencies {
                api(projects.features.videoEngine.spi)
                api(libs.kotlinx.coroutines.core)

                implementation(projects.core.common)
                implementation(projects.features.videoEngine.dash)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        androidMain {
            dependencies {
                implementation(files(unpackLibmpvGlue.flatMap { it.classesJar }))

                implementation(projects.core.i18n)
                implementation(libs.androidx.core)
                // Media3's audio focus, becoming-noisy and lock managers, which ExoPlayer's watched player uses too.
                implementation(libs.androidx.media3.common)
            }
        }
        getByName("desktopMain") {
            dependencies {
                implementation(libs.jna)
                // libmpv for x86_64 Linux, built from the same sources as the Android add-on's
                runtimeOnly(libs.libmpv.desktop)
                // The UI thread players are confined to, and post their state to, is Swing's
                runtimeOnly(libs.kotlinx.coroutines.swing)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.bundles.test)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }
    }
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(unpackLibmpvGlue, UnpackAarGlueTask::assetsDir)
    }
}

dependencies {
    // Its GitHub release has no metadata naming the artifact's type.
    libmpvAar(libs.libmpv.android) {
        artifact { type = "aar" }
    }
}
