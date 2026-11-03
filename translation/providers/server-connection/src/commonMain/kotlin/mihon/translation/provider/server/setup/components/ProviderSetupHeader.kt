package mihon.translation.provider.server.setup.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.ArtworkTile
import mihon.presentation.kit.surface.ArtworkTileSize
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/** The provider's glyph, name and what it does, at the top of its setup screen. */
@Composable
fun ProviderSetupHeader(
    artwork: Painter,
    title: String,
    description: String,
) {
    val spacing = KatariTheme.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkTile(size = ArtworkTileSize.Header) {
            Icon(
                painter = artwork,
                contentDescription = null,
                tone = ContentTone.Secondary,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(spacing.m),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Text(text = title, role = TextRole.SheetTitle)
            Text(text = description, role = TextRole.Body, tone = ContentTone.Secondary)
        }
    }
}
