import app.katari.gradle.Config

plugins {
    alias(katari.plugins.android.library)
    alias(katari.plugins.spotless)
}

android {
    namespace = "app.katari.telemetry"

    sourceSets {
        getByName("main") {
            if (Config.includeTelemetry) {
                kotlin.directories.add("src/firebase/kotlin")
            } else {
                kotlin.directories.add("src/noop/kotlin")
                manifest.srcFile("src/noop/AndroidManifest.xml")
            }
        }
    }
}

dependencies {
    implementation(projects.core.common)

    if (Config.includeTelemetry) {
        implementation(platform(libs.firebase.bom))
        implementation(libs.firebase.analytics)
        implementation(libs.firebase.crashlytics)
    }
}
