package eu.kanade.presentation.browse.components

import androidx.compose.runtime.Composable
import mihon.app.ui.di.LocalAppUiGraph

/**
 * A source row's subtitle: its [language] and, when the extension that provides it came from a store, that store's
 * name, so the same site from two stores can be told apart. Null when there's neither.
 */
@Composable
fun sourceOrigin(language: String?, sourceId: Long): String? {
    val storeName = LocalAppUiGraph.current.sourceRows.storeName(sourceId)
    return listOfNotNull(language?.takeIf { it.isNotBlank() }, storeName)
        .joinToString(" · ")
        .ifEmpty { null }
}
