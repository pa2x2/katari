package app.katari.gradle.tasks

/**
 * Files that hand application host ports to Entry Features: the app's host bindings, which provide them to the
 * graph, and the Entry Feature binding containers, which inject them into the participants that need them.
 */
internal fun isEntryHostCompositionPath(relativePath: String): Boolean {
    return relativePath == APP_ENTRY_HOST_BINDINGS_PATH ||
        (relativePath.startsWith(ENTRY_FEATURE_SOURCE_ROOT) && relativePath.endsWith("Bindings.kt"))
}

internal const val APP_ENTRY_HOST_BINDINGS_PATH =
    "features/entry-interactions/host/src/main/java/app/katari/entry/interactions/host/AppEntryHostBindings.kt"

private const val ENTRY_FEATURE_SOURCE_ROOT = "features/entry-interactions/src/main/java/app/katari/entry/interactions/"

/** The application graphs' declarations and the binding containers they compose, on Android and desktop. */
internal fun isAppGraphCompositionPath(relativePath: String): Boolean {
    return APP_GRAPH_SOURCE_ROOTS.any(relativePath::startsWith) || relativePath == APP_ENTRY_HOST_BINDINGS_PATH
}

private val APP_GRAPH_SOURCE_ROOTS = listOf(
    "app/src/main/java/app/katari/android/di/",
    "desktop/src/main/java/app/katari/desktop/di/",
)
