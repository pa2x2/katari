import mihon.gradle.extensions.desktop

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.compose)
    alias(mihonx.plugins.spotless)
}

kotlin {
    android {
        namespace = "mihon.core.platform"
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.compose.multiplatform.runtime)
                // NetworkConnectivity reports the network as a Flow.
                api(libs.kotlinx.coroutines.core)
                // ImageSharing takes the image as Compose draws it.
                api(libs.compose.multiplatform.ui)
            }
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
