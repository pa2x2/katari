// The desktop web view: Chromium through the JCEF of the JetBrains Runtime the desktop app runs on. The runtime carries
// JCEF's classes and Chromium, so the module compiles against JCEF's published API and ships none of it; on another
// runtime it reports JCEF missing and a page that needs Chromium fails to open.
import app.katari.gradle.jetbrainsruntime.checkJcefApiMatchesJetBrainsRuntime

plugins {
    alias(katari.plugins.kotlin.jvm)
    alias(katari.plugins.compose)
    alias(katari.plugins.spotless)
}

checkJcefApiMatchesJetBrainsRuntime()

dependencies {
    compileOnly(libs.jcef)

    api(projects.core.common)
    api(projects.sources.webView.engine)

    // The browser view the shared web view screen shows, drawn with the kit's menus and tooltips over it
    implementation(projects.appUi)
    implementation(projects.core.presentationCore)
    implementation(projects.core.platform)
    implementation(projects.core.i18n)

    implementation(libs.compose.multiplatform.runtime)
    implementation(libs.compose.multiplatform.foundation)
    implementation(libs.kotlinx.coroutines.core)
    // DevTools messages, which run page scripts and set a page's user agent, are JSON
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.logcat)

    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
