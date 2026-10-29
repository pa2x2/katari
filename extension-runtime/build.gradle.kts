import mihon.gradle.extensioncompatibility.GenerateExtensionFamiliesTask
import mihon.gradle.extensions.compileSharedCodeAgainstAndroidApi
import mihon.gradle.extensions.desktop

// The extension host both apps share: the extension manager, the extension kinds' rules, file extensions, store
// matching and the source manager. What differs per platform (packages, class loading, installing, install events,
// notices, folders, source settings files) comes in through the interfaces in eu.kanade.tachiyomi.extension.platform.
plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.spotless)

    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

// The families stay in the app's folder, where the website and the README table read them too.
val generateExtensionFamilies = tasks.register<GenerateExtensionFamiliesTask>("generateExtensionFamilies") {
    source.set(layout.settingsDirectory.file("app/extension-compatibility.json"))
    packageName.set("eu.kanade.tachiyomi.extension.kind")
    outputDir.set(layout.buildDirectory.dir("generated/extensionFamilies"))
}

kotlin {
    android {
        namespace = "mihon.extension.runtime"

        // TODO(antsy): Remove when https://youtrack.jetbrains.com/issue/KT-83319 is resolved
        withHostTest { }
    }
    desktop()

    sourceSets {
        commonMain {
            kotlin.srcDir(generateExtensionFamilies.flatMap { it.outputDir })
            dependencies {
                implementation(projects.aniyomiSourceApi)
                implementation(projects.backup)
                implementation(projects.core.common)
                implementation(projects.data)
                implementation(projects.domain)
                implementation(projects.entryInteractions.api)
                implementation(projects.entrySourceApi)
                implementation(projects.i18n)
                implementation(projects.sourceApi)
                implementation(projects.sourceCompat)

                implementation(libs.bundles.serialization)
                implementation(libs.injekt)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        getByName("desktopMain") {
            dependencies {
                // The Android classes extensions and the shared host's Android-typed code run on
                implementation(projects.extensionRuntime.androidCompat)
            }
        }
        commonTest {
            dependencies {
                implementation(projects.core.testFixtures)
                implementation(libs.bundles.test)
                implementation(libs.kotlinx.coroutines.test)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }
    }

    compilerOptions {
        // Extensions are handed the app's ProtoBuf instance
        optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
    }
}

// Source settings, the Injekt registrar and the per-profile settings files hand extensions Android's own types
compileSharedCodeAgainstAndroidApi()
