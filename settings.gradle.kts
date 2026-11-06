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
    id("mihon.plugins.android.settings")
}

dependencyResolutionManagement {
    versionCatalogs {
        create("mihonx") {
            from(files("gradle/mihon.versions.toml"))
        }
    }

    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    @Suppress("UnstableApiUsage")
    repositories {
        exclusiveContent {
            // Reduced ONNX Runtime build, published as a GitHub release by the scripts in
            // tools/onnxruntime and checked against gradle/verification-metadata.xml.
            // -PlocalOnnxRuntime reads the copy build-aar.sh stages instead, to try it before publishing.
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
            // video-engine/mpv); the native libraries come from the separately installed mpv add-on.
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
include(":aniyomi-source-api")
include(":app")
include(":app-update")
include(":app-ui")
include(":backup")
include(":baseline-profile")
include(":book-api")
include(":core-metadata")
include(":core:archive")
include(":core:common")
include(":core:image")
include(":core:metro")
include(":core:platform")
include(":core:test-fixtures")
include(":data")
include(":desktop")
include(":domain")
include(":i18n")
include(":library-update")
include(":icons:custom-material-symbols")
include(":icons:material-symbols")
include(":icons:simple-icons")
include(":presentation-core")
include(":presentation-widget")
include(":script-runtime")
include(":entry-interactions")
include(":entry-interactions:api")
include(":entry-interactions:spi")
include(":entry-interactions:download-notification")
include(":entry-interactions:host")
include(":entry-viewer-settings-api")
include(":entry-viewer-chrome-ui")
include(":entry-viewer-settings-ui")
include(":entry-interactions:manga")
include(":entry-interactions:anime")
include(":entry-interactions:book")
include(":entry-source-api")
include(":extension-runtime")
include(":extension-runtime:android-compat")
include(":extension-runtime:jvm-host")
include(":tools:extension-compatibility-harness")
include(":source-api")
include(":source-compat")
include(":source-local")
include(":sync:client")
include(":sync:protocol")
include(":sync:server")
include(":telemetry")
include(":trackers")
include(":text-recognition:api")
include(":text-recognition:spi")
include(":text-recognition:runtime")
include(":text-recognition:ui")
include(":text-recognition:providers:mlkit")
include(":text-recognition:providers:mlkit:catalog")
include(":text-recognition:providers:onnx")
include(":language:api")
include(":language:runtime")
include(":model-artifacts:api")
include(":model-artifacts:runtime")
include(":model-artifacts:ui")
include(":translation:api")
include(":translation:spi")
include(":translation:runtime")
include(":translation:ui")
include(":translation:providers:deepl")
include(":translation:providers:libretranslate")
include(":translation:providers:server-connection")
include(":tts:api")
include(":tts:spi")
include(":tts:runtime")
include(":tts:providers:android")
include(":tts:ui")
include(":video-engine:dash")
include(":video-engine:mpv")
include(":video-engine:spi")
include(":web-view:jcef")
include(":showcase:capture")
include(":showcase:extension")
