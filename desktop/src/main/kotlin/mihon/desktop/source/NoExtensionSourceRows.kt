package mihon.desktop.source

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.browse.components.SourceRows
import mihon.presentation.kit.content.LogoFrame
import mihon.presentation.kit.content.LogoSize
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.surface.LetterTile
import tachiyomi.domain.source.model.Source

/**
 * Desktop loads no extensions yet, so a source has no icon or store to show: it is drawn with its initial, as Android
 * draws a source whose extension isn't installed. Stands in until desktop hosts extensions.
 */
@Inject
class NoExtensionSourceRows : SourceRows {
    @Composable
    override fun Icon(source: Source, modifier: Modifier) {
        LogoFrame(size = LogoSize.Row, modifier = modifier) {
            LetterTile(name = source.name.ifBlank { source.id.toString() }, modifier = Modifier.fillMaxSize())
        }
    }

    @Composable
    override fun Row(source: Source, onClick: () -> Unit) {
        SettingsRow(
            title = source.name,
            subtitle = source.lang.takeIf { it.isNotBlank() },
            leading = { Icon(source = source, modifier = Modifier) },
            onClick = onClick,
        )
    }
}
