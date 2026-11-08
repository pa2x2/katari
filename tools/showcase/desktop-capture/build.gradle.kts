// The desktop scenes: a Java agent the showcase pipeline loads into the desktop app it captures, through
// JAVA_TOOL_OPTIONS, so the app itself carries no capture code. The agent finds controls through the windows'
// accessibility trees and Compose semantics, clicks and types with AWT's Robot, and captures windows from the screen.
// tools/showcase/pipeline/desktop starts the app with it.
plugins {
    alias(katari.plugins.kotlin.jvm)
    alias(katari.plugins.spotless)
}

tasks.jar {
    manifest {
        attributes("Premain-Class" to "app.katari.showcase.desktop.CaptureAgent")
    }
}

dependencies {
    // The app loads the agent onto its own class path, which brings its Compose and Kotlin.
    compileOnly(libs.compose.multiplatform.ui)
}
