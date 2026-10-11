plugins {
    alias(mihonx.plugins.android.application)
    alias(mihonx.plugins.spotless)

    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.katari.showcase.extension"

    defaultConfig {
        applicationId = "app.katari.showcase.extension"
        versionCode = 1
        // The extension loader reads the source API version from versionName's major.minor.
        versionName = "2.6.1"

        // showcase/content/content-server.ts: the content server on the host, as the emulator reaches it.
        buildConfigField("String", "CONTENT_SERVER", "\"http://10.0.2.2:8571\"")
    }

    buildFeatures {
        buildConfig = true
    }

    sourceSets {
        // The factory lists its sources from the content's source list before any network request is possible.
        getByName("main").resources.directories.add("../content/source-list")
    }
}

// Katari provides the source API and the Kotlin runtime to every extension it loads; bundled copies would shadow them.
configurations.configureEach {
    if (name.endsWith("RuntimeClasspath")) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    }
}

dependencies {
    compileOnly(projects.entrySourceApi)
}
