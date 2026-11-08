package app.katari.translation.ui.picker.engine

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.surface.ArtworkTile
import app.katari.presentation.kit.surface.ArtworkTileSize
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.translation.api.engine.TranslationEngineArtwork
import org.jetbrains.compose.resources.painterResource

/** An engine's logo on a rounded tile. */
@Composable
internal expect fun TranslationEngineArtwork(
    artwork: TranslationEngineArtwork,
    size: ArtworkTileSize,
    modifier: Modifier = Modifier,
)

/** [artwork] in the colour of secondary text, with the wider inset a glyph gets. */
@Composable
internal fun TranslationEngineGlyph(
    artwork: TranslationEngineArtwork.Glyph,
    size: ArtworkTileSize,
    modifier: Modifier,
) {
    ArtworkTile(size = size, modifier = modifier) {
        Icon(
            painter = painterResource(artwork.resource),
            contentDescription = null,
            tone = ContentTone.Secondary,
            modifier = Modifier
                .fillMaxSize()
                .padding(KatariTheme.spacing.m),
        )
    }
}
