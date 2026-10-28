package eu.kanade.presentation.browse.components

import android.util.DisplayMetrics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import mihon.presentation.kit.content.LogoSize
import tachiyomi.domain.source.model.Source

/** The icons and stores of the extensions Android loads. */
@Inject
class AndroidSourceRows(
    private val extensionManager: ExtensionManager,
) : SourceRows {
    @Composable
    override fun Icon(source: Source, size: LogoSize, modifier: Modifier) {
        SourceIcon(source = source, modifier = modifier, size = size)
    }

    @Composable
    override fun Icon(sourceId: Long, name: String, modifier: Modifier) {
        SourceIcon(sourceId = sourceId, modifier = modifier)
    }

    @Composable
    override fun ExtensionIcon(extension: Extension, size: LogoSize, modifier: Modifier) {
        // The details' header draws it large enough to need the package's sharpest icon
        val density = if (size == LogoSize.Header) DisplayMetrics.DENSITY_XXXHIGH else DisplayMetrics.DENSITY_DEFAULT
        ExtensionIcon(extension = extension, modifier = modifier, size = size, density = density)
    }

    @Composable
    override fun storeName(sourceId: Long): String? {
        val storeNames by extensionManager.sourceStoreNames.collectAsState()
        return storeNames[sourceId]
    }
}
