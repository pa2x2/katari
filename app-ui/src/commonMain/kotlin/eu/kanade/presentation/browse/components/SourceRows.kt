package eu.kanade.presentation.browse.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tachiyomi.domain.source.model.Source

/**
 * How shared screens draw a source: its icon, and its row in a list of sources. Both show what the extension that
 * provides the source carries, such as its icon and the store it came from, which each platform loads its own way.
 */
interface SourceRows {
    @Composable
    fun Icon(source: Source, modifier: Modifier)

    /** The source's row with its icon, name, language and store, and the entry types it supplies. */
    @Composable
    fun Row(source: Source, onClick: () -> Unit)
}
