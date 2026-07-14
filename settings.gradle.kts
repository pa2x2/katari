pluginManagement {
    includeBuild("gradle/build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://www.jitpack.io")
    }
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
        google()
        mavenCentral()
        maven(url = "https://www.jitpack.io")
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "Katari"
include(":app")
include(":baseline-profile")
include(":book-api")
include(":core-metadata")
include(":core:archive")
include(":core:common")
include(":data")
include(":domain")
include(":i18n")
include(":presentation-core")
include(":presentation-widget")
include(":entry-interactions")
include(":entry-interactions:api")
include(":entry-interactions:spi")
include(":entry-viewer-settings-api")
include(":entry-interactions:manga")
include(":entry-interactions:anime")
include(":entry-interactions:book")
include(":entry-source-api")
include(":source-api")
include(":source-compat")
include(":source-local")
include(":telemetry")
