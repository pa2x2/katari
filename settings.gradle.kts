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
        if (providers.gradleProperty("useLocalImageDecoder").orNull.toBoolean()) {
            mavenLocal {
                content {
                    includeModule("com.github.pa2x2", "image-decoder")
                }
            }
        }
        exclusiveContent {
            // Reduced ONNX Runtime build, published as a GitHub release by the scripts in
            // text-recognition/providers/onnx/onnxruntime and checked against gradle/verification-metadata.xml.
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
            filter { includeGroup("app.katari.libmpv") }
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
        google()
        mavenCentral()
        maven(url = "https://www.jitpack.io")
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "Katari"
include(":aniyomi-source-api")
include(":app")
include(":baseline-profile")
include(":book-api")
include(":core-metadata")
include(":core:archive")
include(":core:common")
include(":core:metro")
include(":data")
include(":domain")
include(":i18n")
include(":icons:custom-material-symbols")
include(":icons:material-symbols")
include(":icons:simple-icons")
include(":presentation-core")
include(":presentation-widget")
include(":entry-interactions")
include(":entry-interactions:api")
include(":entry-interactions:spi")
include(":entry-interactions:download-notification")
include(":entry-viewer-settings-api")
include(":entry-viewer-chrome-ui")
include(":entry-viewer-settings-ui")
include(":entry-interactions:manga")
include(":entry-interactions:anime")
include(":entry-interactions:book")
include(":entry-source-api")
include(":source-api")
include(":source-compat")
include(":source-local")
include(":telemetry")
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
include(":showcase:capture")
include(":showcase:extension")
