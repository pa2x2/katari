pluginManagement {
    includeBuild("gradle/build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://www.jitpack.io")
    }
}

plugins {
    id("app.katari.plugins.android.settings")
}

dependencyResolutionManagement {
    versionCatalogs {
        create("katari") {
            from(files("gradle/katari.versions.toml"))
        }
    }

    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    @Suppress("UnstableApiUsage")
    repositories {
        exclusiveContent {
            // Reduced ONNX Runtime builds, the Android AAR and the desktop app's Linux jar, published together
            // as a GitHub release by the scripts in tools/onnxruntime and checked against
            // gradle/verification-metadata.xml. -PlocalOnnxRuntime reads the copies build.sh stages instead, to
            // try them before publishing.
            forRepository {
                ivy {
                    name = "KatariOnnxRuntime"
                    url = if (providers.gradleProperty("localOnnxRuntime").orNull.toBoolean()) {
                        val work = providers.environmentVariable("ONNXRUNTIME_BUILD_DIR")
                            .orElse("${System.getProperty("user.home")}/.cache/katari-onnxruntime")
                        file("${work.get()}/release").toURI()
                    } else {
                        uri("https://github.com/pa2x2/katari/releases/download")
                    }
                    patternLayout { artifact("onnxruntime-[revision]/[artifact]-[revision].[ext]") }
                    metadataSources { artifact() }
                }
            }
            filter { includeGroup("app.katari.onnxruntime") }
        }
        exclusiveContent {
            // libarchive for the desktop app on x86_64 Linux, with its compression libraries built in, published as a
            // GitHub release by the scripts in tools/libarchive and checked against gradle/verification-metadata.xml.
            // -PlocalLibarchive=true reads the copy build-jar.sh stages instead, to try it before publishing.
            forRepository {
                ivy {
                    name = "KatariLibarchive"
                    url = if (providers.gradleProperty("localLibarchive").orNull.toBoolean()) {
                        val work = providers.environmentVariable("LIBARCHIVE_BUILD_DIR")
                            .orElse("${System.getProperty("user.home")}/.cache/katari-libarchive")
                        file("${work.get()}/release").toURI()
                    } else {
                        uri("https://github.com/pa2x2/katari/releases/download")
                    }
                    patternLayout { artifact("libarchive-[revision]/[artifact]-[revision].[ext]") }
                    metadataSources { artifact() }
                }
            }
            filter { includeGroup("app.katari.libarchive") }
        }
        exclusiveContent {
            // LGPL libmpv with its JNI glue, released by pa2x2/mpv-android and checked against
            // gradle/verification-metadata.xml. Katari takes only the glue's classes and assets from it (see
            // features/video-engine/mpv); the native libraries come from the separately installed mpv add-on.
            forRepository {
                ivy {
                    name = "KatariLibmpv"
                    url = uri("https://github.com/pa2x2/mpv-android/releases/download")
                    patternLayout { artifact("libmpv-[revision]/[artifact]-[revision].[ext]") }
                    metadataSources { artifact() }
                }
            }
            filter { includeModule("app.katari.libmpv", "libmpv-android") }
        }
        exclusiveContent {
            // LGPL libmpv for the desktop app on x86_64 Linux, built from the same fork release's sources, published
            // as a GitHub release by the scripts in tools/libmpv and checked against gradle/verification-metadata.xml.
            // -PlocalLibmpv=true reads the copy build-jar.sh stages instead, to try it before publishing.
            forRepository {
                ivy {
                    name = "KatariLibmpvDesktop"
                    url = if (providers.gradleProperty("localLibmpv").orNull.toBoolean()) {
                        val work = providers.environmentVariable("LIBMPV_BUILD_DIR")
                            .orElse("${System.getProperty("user.home")}/.cache/katari-libmpv")
                        file("${work.get()}/release").toURI()
                    } else {
                        uri("https://github.com/pa2x2/katari/releases/download")
                    }
                    patternLayout { artifact("libmpv-desktop-[revision]/[artifact]-[revision].[ext]") }
                    metadataSources { artifact() }
                }
            }
            filter { includeModule("app.katari.libmpv", "libmpv-linux-x86-64") }
        }
        exclusiveContent {
            // The image decoder for Android and the desktop app, released by pa2x2/image-decoder and checked against
            // gradle/verification-metadata.xml. -PlocalImageDecoder reads the fork's publishToMavenLocal output
            // instead (with --dependency-verification=lenient), to try a change before it is released.
            forRepository {
                if (providers.gradleProperty("localImageDecoder").orNull.toBoolean()) {
                    mavenLocal()
                } else {
                    ivy {
                        name = "ImageDecoder"
                        url = uri("https://github.com/pa2x2/image-decoder/releases/download")
                        patternLayout { artifact("image-decoder-[revision]/[artifact]-[revision].[ext]") }
                        metadataSources { artifact() }
                    }
                }
            }
            filter {
                includeModule("io.github.pa2x2", "image-decoder-android")
                includeModule("io.github.pa2x2", "image-decoder-jvm")
            }
        }
        exclusiveContent {
            // quickjs-kt with fixes for interrupting and failing evaluations, released by pa2x2/quickjs-kt and
            // checked against gradle/verification-metadata.xml. -PlocalQuickJsKt reads the fork's
            // publishToMavenLocal output instead (with --dependency-verification=lenient), to try a change before
            // it is released.
            forRepository {
                if (providers.gradleProperty("localQuickJsKt").orNull.toBoolean()) {
                    mavenLocal()
                } else {
                    ivy {
                        name = "QuickJsKt"
                        url = uri("https://github.com/pa2x2/quickjs-kt/releases/download")
                        patternLayout { artifact("quickjs-kt-[revision]/[artifact]-[revision].[ext]") }
                        metadataSources { artifact() }
                    }
                }
            }
            filter {
                includeModule("io.github.pa2x2", "quickjs-kt-android")
                includeModule("io.github.pa2x2", "quickjs-kt-jvm")
            }
        }
        exclusiveContent {
            // JCEF's API, which the desktop web view compiles against; the JetBrains Runtime the desktop app runs on
            // carries the classes themselves. Checked against gradle/verification-metadata.xml.
            forRepository {
                maven {
                    name = "IntellijDependencies"
                    url = uri("https://cache-redirector.jetbrains.com/intellij-dependencies")
                }
            }
            filter { includeGroup("org.jetbrains.intellij.deps.jcef") }
        }
        google()
        mavenCentral()
        maven(url = "https://www.jitpack.io")
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "Katari"

include(":app", ":app-ui", ":desktop")

include(
    ":core:archive",
    ":core:common",
    ":core:core-metadata",
    ":core:data",
    ":core:domain",
    ":core:i18n",
    ":core:icons:custom-material-symbols",
    ":core:icons:material-symbols",
    ":core:icons:simple-icons",
    ":core:image",
    ":core:metro",
    ":core:platform",
    ":core:presentation-core",
    ":core:telemetry",
    ":core:test-fixtures",
)

include(
    ":sources:aniyomi-source-api",
    ":sources:book-api",
    ":sources:entry-source-api",
    ":sources:extension-runtime",
    ":sources:extension-runtime:android-compat",
    ":sources:extension-runtime:jvm-host",
    ":sources:script-runtime",
    ":sources:source-api",
    ":sources:source-compat",
    ":sources:source-local",
    ":sources:web-view:jcef",
)

include(
    ":features:app-update",
    ":features:backup",
    ":features:entry-interactions",
    ":features:entry-interactions:anime",
    ":features:entry-interactions:api",
    ":features:entry-interactions:book",
    ":features:entry-interactions:download-notification",
    ":features:entry-interactions:host",
    ":features:entry-interactions:manga",
    ":features:entry-interactions:spi",
    ":features:entry-viewer-chrome-ui",
    ":features:entry-viewer-settings-api",
    ":features:entry-viewer-settings-ui",
    ":features:language:api",
    ":features:language:runtime",
    ":features:library-update",
    ":features:model-artifacts:api",
    ":features:model-artifacts:runtime",
    ":features:model-artifacts:ui",
    ":features:presentation-widget",
    ":features:sync:client",
    ":features:sync:protocol",
    ":features:sync:server",
    ":features:text-recognition:api",
    ":features:text-recognition:providers:mlkit",
    ":features:text-recognition:providers:mlkit:catalog",
    ":features:text-recognition:providers:mlkit:not-included",
    ":features:text-recognition:providers:onnx",
    ":features:text-recognition:runtime",
    ":features:text-recognition:spi",
    ":features:text-recognition:ui",
    ":features:translation:api",
    ":features:translation:providers:deepl",
    ":features:translation:providers:libretranslate",
    ":features:translation:providers:server-connection",
    ":features:translation:runtime",
    ":features:translation:spi",
    ":features:translation:ui",
    ":features:tts:api",
    ":features:tts:providers:android",
    ":features:tts:providers:speech-dispatcher",
    ":features:tts:runtime",
    ":features:tts:spi",
    ":features:tts:ui",
    ":features:video-engine:dash",
    ":features:video-engine:mpv",
    ":features:video-engine:spi",
)

include(
    ":tools:baseline-profile",
    ":tools:extension-compatibility-harness",
    ":tools:showcase:capture",
    ":tools:showcase:desktop-capture",
    ":tools:showcase:extension",
    ":tools:showcase:sync-server",
)
