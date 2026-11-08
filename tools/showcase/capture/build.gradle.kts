plugins {
    alias(katari.plugins.android.test)
    alias(katari.plugins.spotless)
}

android {
    namespace = "app.katari.showcase.capture"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = projects.app.path

    buildTypes {
        // Captures run against the app's release-like benchmark build, which is debug-signed like this APK. The build
        // type is named after its job, so assembleBenchmark and installBenchmark across the whole build leave it out.
        create("showcase") {
            isDebuggable = true
            signingConfig = getByName("debug").signingConfig
            matchingFallbacks += listOf("benchmark", "release")
        }
    }

    // Scenes restart the app, which would end an instrumentation running inside its process.
    @Suppress("UnstableApiUsage")
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

androidComponents {
    beforeVariants { variant ->
        variant.enable = variant.buildType == "showcase"
    }
}

dependencies {
    implementation(libs.androidx.test.junit)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.uiautomator)
}
