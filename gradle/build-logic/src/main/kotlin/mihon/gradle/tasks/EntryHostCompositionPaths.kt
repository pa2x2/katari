package mihon.gradle.tasks

/**
 * Files that hand application host ports to Entry Features: the app's host bindings, which provide them to the
 * graph, and the Entry Feature binding containers, which inject them into the participants that need them.
 */
internal fun isEntryHostCompositionPath(relativePath: String): Boolean {
    return relativePath == APP_ENTRY_HOST_BINDINGS_PATH ||
        (relativePath.startsWith(ENTRY_FEATURE_SOURCE_ROOT) && relativePath.endsWith("Bindings.kt"))
}

internal const val APP_ENTRY_HOST_BINDINGS_PATH =
    "app/src/main/java/mihon/entry/interactions/host/AppEntryHostBindings.kt"

private const val ENTRY_FEATURE_SOURCE_ROOT = "entry-interactions/src/main/java/mihon/entry/interactions/"
