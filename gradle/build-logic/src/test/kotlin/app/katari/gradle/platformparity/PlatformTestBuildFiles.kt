package app.katari.gradle.platformparity

/** A shared module that both apps depend on: the smallest layout the platform rules can tell the platforms apart in. */
internal val APP_BUILD_FILES = listOf(
    PlatformBuildFile("feature", "plugins {\n    alias(katari.plugins.kotlin.multiplatform)\n}"),
    PlatformBuildFile(
        "app",
        "plugins {\n    alias(katari.plugins.android.application)\n}\n\n" +
            "dependencies {\n    implementation(projects.feature)\n}",
    ),
    PlatformBuildFile(
        "desktop",
        "plugins {\n    alias(katari.plugins.desktop.application)\n}\n\n" +
            "dependencies {\n    implementation(projects.feature)\n}",
    ),
)
