package eu.kanade.tachiyomi.extension.util

import eu.kanade.domain.source.service.SourcePreferences
import mihon.domain.extension.model.ContentWarning
import mihon.domain.extension.model.ExtensionStore

/**
 * What decides whether an extension may load, read once for every extension loaded together.
 */
internal data class ExtensionLoadPolicy(
    val stores: List<ExtensionStore>,
    val enabledContentWarnings: Set<ContentWarning>,
    val applyContentWarningsToInstalled: Boolean,
) {
    fun filters(contentWarning: ContentWarning): Boolean {
        return applyContentWarningsToInstalled && contentWarning !in enabledContentWarnings
    }

    companion object {
        fun read(preferences: SourcePreferences, stores: List<ExtensionStore>) = ExtensionLoadPolicy(
            stores = stores,
            enabledContentWarnings = preferences.enabledContentWarnings.get(),
            applyContentWarningsToInstalled = preferences.applyContentWarningsToInstalled.get(),
        )
    }
}

/**
 * The message of the deepest cause, which is the one that actually says what went wrong.
 */
internal val Throwable.rootMessage: String
    get() {
        val root = generateSequence(this) { it.cause }.last()
        return listOfNotNull(root::class.simpleName, root.message).joinToString(": ")
    }
