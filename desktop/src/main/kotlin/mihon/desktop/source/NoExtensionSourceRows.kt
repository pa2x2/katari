package mihon.desktop.source

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.browse.components.SourceRows
import mihon.presentation.kit.content.LogoFrame
import mihon.presentation.kit.content.LogoSize
import mihon.presentation.kit.surface.LetterTile
import tachiyomi.domain.source.model.Source

/**
 * Desktop loads no extensions yet, so a source has no icon or store to show: it is drawn with its initial, as Android
 * draws a source whose extension isn't installed. Stands in until desktop hosts extensions.
 */
@Inject
class NoExtensionSourceRows : SourceRows {
    @Composable
    override fun Icon(source: Source, size: LogoSize, modifier: Modifier) {
        LetterIcon(name = source.name.ifBlank { source.id.toString() }, size = size, modifier = modifier)
    }

    @Composable
    override fun Icon(sourceId: Long, name: String, modifier: Modifier) {
        LetterIcon(name = name.ifBlank { sourceId.toString() }, size = LogoSize.Row, modifier = modifier)
    }

    @Composable
    override fun storeName(sourceId: Long): String? = null

    @Composable
    private fun LetterIcon(name: String, size: LogoSize, modifier: Modifier) {
        LogoFrame(size = size, modifier = modifier) {
            LetterTile(name = name, modifier = Modifier.fillMaxSize())
        }
    }
}
