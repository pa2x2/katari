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
                api(projects.videoEngine.spi)
                api(libs.kotlinx.coroutines.core)

                implementation(projects.core.common)
                implementation(projects.videoEngine.dash)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        androidMain {
            dependencies {
                implementation(files(unpackLibmpvGlue.flatMap { it.classesJar }))

                implementation(projects.i18n)
                implementation(libs.androidx.core)
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
