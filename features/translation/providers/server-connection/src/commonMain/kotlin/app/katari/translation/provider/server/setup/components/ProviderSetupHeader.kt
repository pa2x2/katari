package app.katari.translation.provider.server.setup.components

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
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.surface.ArtworkTile
import app.katari.presentation.kit.surface.ArtworkTileSize
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole

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
