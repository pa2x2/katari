// Speech through Speech Dispatcher, the speech service Linux desktops share. The module speaks Speech Dispatcher's own
// text protocol over its socket, so it ships no native library, and starts the service the way its client library does.
plugins {
    alias(mihonx.plugins.kotlin.jvm)
    alias(mihonx.plugins.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(mihonx.plugins.spotless)

    alias(libs.plugins.metro)
}

dependencies {
    implementation(projects.core.i18n)
    implementation(projects.features.tts.api)
    implementation(projects.features.tts.spi)
    // The engine's glyph
    implementation(libs.compose.multiplatform.resources)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.bundles.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

compose.resources {
    packageOfResClass = "mihon.tts.provider.speechdispatcher.resources"
}
