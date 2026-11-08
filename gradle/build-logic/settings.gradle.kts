dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../libs.versions.toml"))
        }
        create("katari") {
            from(files("../katari.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":settings")
