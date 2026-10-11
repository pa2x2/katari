plugins {
    alias(mihonx.plugins.android.test)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "app.katari.showcase.capture"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = projects.app.path

    buildTypes {
        // Captures run against the release-like benchmark build, which is debug-signed like this APK.
        create("benchmark") {
            isDebuggable = true
            signingConfig = getByName("debug").signingConfig
            matchingFallbacks += "release"
        }
    }

    // Scenes restart the app, which would end an instrumentation running inside its process.
    @Suppress("UnstableApiUsage")
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

androidComponents {
    beforeVariants { variant ->
        variant.enable = variant.buildType == "benchmark"
    }
}

dependencies {
    implementation(libs.androidx.test.junit)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.uiautomator)
}
