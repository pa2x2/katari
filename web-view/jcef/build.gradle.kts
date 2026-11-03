// The desktop web view: Chromium through the JCEF of the JetBrains Runtime the desktop app runs on. The runtime carries
// JCEF's classes and Chromium, so the module compiles against JCEF's published API and ships none of it; on another
// runtime it reports JCEF missing and the app keeps its web view hidden.
import mihon.gradle.jetbrainsruntime.checkJcefApiMatchesJetBrainsRuntime

plugins {
    alias(mihonx.plugins.kotlin.jvm)
    alias(mihonx.plugins.compose)
    alias(mihonx.plugins.spotless)
}

checkJcefApiMatchesJetBrainsRuntime()

dependencies {
    compileOnly(libs.jcef)

    api(projects.core.common)

    // The browser view the shared web view screen shows, drawn with the kit's menus and tooltips over it
    implementation(projects.appUi)
    implementation(projects.presentationCore)
    implementation(projects.core.platform)
    implementation(projects.i18n)

    implementation(libs.compose.multiplatform.runtime)
    implementation(libs.compose.multiplatform.foundation)
    implementation(libs.kotlinx.coroutines.core)
    // DevTools messages, which run page scripts and set a page's user agent, are JSON
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.logcat)

    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
