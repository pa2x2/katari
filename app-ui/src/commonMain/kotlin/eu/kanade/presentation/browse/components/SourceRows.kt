package eu.kanade.presentation.browse.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.content.LogoSize
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.coil.ExtensionIconDensity
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import tachiyomi.domain.source.model.Source

/**
 * What source and extension rows show of the extensions behind them: their icons, as [SourceIcon] and [ExtensionIcon]
 * draw them, and the store they came from. [BaseSourceItem] builds a whole source row from them.
 */
@Inject
class SourceRows(
    private val extensionManager: ExtensionManager,
) {
    @Composable
    fun Icon(source: Source, size: LogoSize, modifier: Modifier) {
        SourceIcon(source = source, modifier = modifier, size = size)
    }

    /** The icon of the source with [sourceId], for a list that has only its id. */
    @Composable
    fun Icon(sourceId: Long, modifier: Modifier) {
        SourceIcon(sourceId = sourceId, modifier = modifier)
    }

    /** The icon of [extension]: its store's for a listing, its own once installed, a warning for one not loaded. */
    @Composable
    fun ExtensionIcon(extension: Extension, size: LogoSize, modifier: Modifier) {
        // The details' header draws it large enough to need the package's sharpest icon
        val density = if (size == LogoSize.Header) ExtensionIconDensity.Sharpest else ExtensionIconDensity.Baseline
        ExtensionIcon(extension = extension, modifier = modifier, size = size, density = density)
    }

    /** The store the extension that provides [sourceId] came from, or null for a sideloaded one or none. */
    @Composable
    fun storeName(sourceId: Long): String? {
        val storeNames by extensionManager.sourceStoreNames.collectAsState()
        return storeNames[sourceId]
    }
}
