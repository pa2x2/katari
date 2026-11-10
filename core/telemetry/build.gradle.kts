import app.katari.gradle.Config
import app.katari.gradle.extensions.desktop
import app.katari.gradle.telemetry.GeneratePostHogProjectTokenTask

plugins {
    alias(katari.plugins.kotlin.multiplatform)
    alias(katari.plugins.spotless)
}

val generatePostHogProjectToken = tasks.register<GeneratePostHogProjectTokenTask>("generatePostHogProjectToken") {
    packageName.set("app.katari.telemetry")
    token.set(Config.postHogProjectToken)
    outputDir.set(layout.buildDirectory.dir("generated/postHogProjectToken"))
}

kotlin {
    android {
        namespace = "app.katari.telemetry"

        withHostTest { }
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core.common)
            }
        }
        getByName("androidHostTest") {
            dependencies {
                implementation(libs.bundles.test)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }

        // Only builds made with -Pinclude-telemetry carry PostHog. The rest, the FOSS build among them, get a Telemetry
        // that sends nothing and keep PostHog's libraries out of the app. Of those that carry it, only the ones given
        // the project's token, which the release workflow alone passes, send anything.
        if (Config.includeTelemetry) {
            commonMain {
                kotlin.srcDir("src/posthog/commonMain/kotlin")
                kotlin.srcDir(generatePostHogProjectToken.flatMap { it.outputDir })
            }
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
