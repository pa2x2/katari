import mihon.gradle.tasks.UnpackAarGlueTask

plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

android {
    namespace = "mihon.video.engine.mpv"

    defaultConfig {
        consumerProguardFiles("consumer-proguard.pro")
    }
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
    implementation(files(unpackLibmpvGlue.flatMap { it.classesJar }))

    api(projects.videoEngine.spi)
    api(libs.kotlinx.coroutines.core)

    implementation(projects.core.common)
    implementation(projects.i18n)
    implementation(libs.androidx.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
