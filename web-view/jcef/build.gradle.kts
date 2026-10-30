// The desktop web view: Chromium through the JCEF of the JetBrains Runtime the desktop app runs on. The runtime carries
// JCEF's classes and Chromium, so the module compiles against JCEF's published API and ships none of it; on another
// runtime it reports JCEF missing and the app keeps its web view hidden.
import mihon.gradle.jetbrainsruntime.checkJcefApiMatchesJetBrainsRuntime

plugins {
    alias(mihonx.plugins.kotlin.jvm)
    alias(mihonx.plugins.spotless)
}

checkJcefApiMatchesJetBrainsRuntime()

dependencies {
    compileOnly(libs.jcef)

    api(projects.core.common)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.logcat)

    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
