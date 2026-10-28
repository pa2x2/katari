package eu.kanade.presentation.browse.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.extension.ExtensionManager
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
    override fun storeName(sourceId: Long): String? {
        val storeNames by extensionManager.sourceStoreNames.collectAsState()
        return storeNames[sourceId]
    }
}
