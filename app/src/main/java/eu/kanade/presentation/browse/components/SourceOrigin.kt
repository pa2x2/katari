package eu.kanade.presentation.browse.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import mihon.app.di.appGraph

/**
 * A source row's subtitle: its [language] and, when the extension that provides it came from a store, that store's
 * name, so the same site from two stores can be told apart. Null when there's neither.
 */
@Composable
internal fun sourceOrigin(language: String?, sourceId: Long): String? {
    val context = LocalContext.current
    val storeNames by remember(context) { context.appGraph.extensionManager.sourceStoreNames }.collectAsState()
    return listOfNotNull(language?.takeIf { it.isNotBlank() }, storeNames[sourceId])
        .joinToString(" · ")
        .ifEmpty { null }
}
