package eu.kanade.presentation.browse.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.extension.model.Extension
import mihon.presentation.kit.content.LogoSize
import tachiyomi.domain.source.model.Source

/**
 * What source and extension rows show of the extensions behind them, such as their icons and the store they came from,
 * which each platform loads its own way. [BaseSourceItem] builds a whole source row from them.
 */
interface SourceRows {
    @Composable
    fun Icon(source: Source, size: LogoSize, modifier: Modifier)

    /** The icon of the source with [sourceId], for a list that has only its id and [name]. */
    @Composable
    fun Icon(sourceId: Long, name: String, modifier: Modifier)

    /** The icon of [extension]: its store's for a listing, its own once installed, a warning for one not loaded. */
    @Composable
    fun ExtensionIcon(extension: Extension, size: LogoSize, modifier: Modifier)

    /** The store the extension that provides [sourceId] came from, or null for a sideloaded one or none. */
    @Composable
    fun storeName(sourceId: Long): String?
}
