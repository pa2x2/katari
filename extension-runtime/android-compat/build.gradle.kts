import mihon.gradle.extensions.androidPlatformJar
import mihon.gradle.tasks.AndroidStubJarTask

// The Android classes extensions call, for the desktop extension host only: stand-ins copied from Suwayomi's
// AndroidCompat and Katari's androidx.preference model (README.md), over a stub of the SDK's android.jar for every
// class neither covers. Copied sources stay in src/main/java as they were; Katari's own code is in src/main/kotlin.
plugins {
    alias(mihonx.plugins.kotlin.jvm)
    alias(mihonx.plugins.spotless)
}

val androidStubJar = tasks.register<AndroidStubJarTask>("androidStubJar") {
    androidJar.set(androidPlatformJar())
    sourceRoots.from("src/main/java", "src/main/kotlin")
    stubJar.set(layout.buildDirectory.file("android-stub/android-stub.jar"))
}
val androidStub = files(androidStubJar)

dependencies {
    // Every Android class the stand-ins don't implement still loads, and throws "Stub!" only when called. Desktop code
    // that names Android classes compiles against the same.
    api(androidStub)
    // The pages extensions' web views run on, which the host hands over with the environment
    api(projects.core.common)

    implementation(libs.icu4j)
    implementation(libs.jsoup)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.logcat)
    implementation(libs.rxJava)

    // ImageIO plugins for what BitmapFactory decodes on Android and the JDK can't: CMYK and Adobe JPEGs, and WebP
    runtimeOnly(libs.bundles.twelvemonkeys.imageio)

    compileOnly(libs.androidx.annotation)

    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

spotless {
    kotlin {
        // Copied Kotlin keeps its upstream formatting, as the copied Java does.
        targetExclude("src/main/java/**")
    }
}
