package eu.kanade.presentation.browse.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.content.LogoSize
import tachiyomi.domain.source.model.Source

/**
 * What a source's rows show of the extension that provides it, such as its icon and the store it came from, which
 * each platform loads its own way. [BaseSourceItem] builds a whole row from them.
 */
interface SourceRows {
    @Composable
    fun Icon(source: Source, size: LogoSize, modifier: Modifier)

    /** The icon of the source with [sourceId], for a list that has only its id and [name]. */
    @Composable
    fun Icon(sourceId: Long, name: String, modifier: Modifier)

    /** The store the extension that provides [sourceId] came from, or null for a sideloaded one or none. */
    @Composable
    fun storeName(sourceId: Long): String?
}
