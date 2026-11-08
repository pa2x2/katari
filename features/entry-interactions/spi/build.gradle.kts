import mihon.gradle.extensions.desktop

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.compose)
    alias(mihonx.plugins.spotless)
    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "app.katari.entry.interactions.spi"

        withHostTest { }
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.features.entryInteractions.api)
                api(libs.coil.core)

                implementation(projects.core.common)
                implementation(projects.core.metro)
                implementation(projects.core.platform)
                implementation(projects.core.i18n)
                implementation(projects.core.icons.materialSymbols)
                implementation(projects.core.presentationCore)
            }
        }
        getByName("desktopMain") {
            dependencies {
                // Copying an image to the clipboard decodes it the way the readers draw it.
                implementation(projects.core.image)
            }
        }
        androidMain {
            dependencies {
                api(libs.androidx.appCompat)

                implementation(libs.androidx.activity.compose)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.bundles.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(kotlin("test"))
                runtimeOnly(libs.junit.platform.launcher)
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    // MockK/Byte Buddy instruments the test JVM, which is incompatible with class-data sharing.
    jvmArgs("-XX:+EnableDynamicAgentLoading", "-Xshare:off")
}
