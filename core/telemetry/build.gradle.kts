import app.katari.gradle.Config
import app.katari.gradle.extensions.desktop

plugins {
    alias(katari.plugins.kotlin.multiplatform)
    alias(katari.plugins.spotless)
}

kotlin {
    android {
        namespace = "app.katari.telemetry"
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core.common)
            }
        }

        // Only builds made with -Pinclude-telemetry carry PostHog. The rest, the FOSS build among them, get a Telemetry
        // that sends nothing and keep PostHog's libraries out of the app.
        if (Config.includeTelemetry) {
            commonMain { kotlin.srcDir("src/posthog/commonMain/kotlin") }
            androidMain {
                kotlin.srcDir("src/posthog/androidMain/kotlin")
                dependencies {
                    implementation(libs.posthog.android)
                    implementation(libs.androidx.core)
                }
            }
            getByName("desktopMain") {
                kotlin.srcDir("src/posthog/desktopMain/kotlin")
                dependencies {
                    implementation(libs.posthog.kmp)
                }
            }
        } else {
            commonMain { kotlin.srcDir("src/noop/commonMain/kotlin") }
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
