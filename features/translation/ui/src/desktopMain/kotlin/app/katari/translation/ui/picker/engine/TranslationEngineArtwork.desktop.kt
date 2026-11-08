package app.katari.translation.ui.picker.engine

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.surface.ArtworkTile
import app.katari.presentation.kit.surface.ArtworkTileSize
import app.katari.translation.api.engine.TranslationEngineArtwork

/**
 * The glyph, or an empty tile for artwork that names Android drawables and application packages, which only engines
 * of Android's own have.
 */
@Composable
internal actual fun TranslationEngineArtwork(
    artwork: TranslationEngineArtwork,
    size: ArtworkTileSize,
    modifier: Modifier,
) {
    when (artwork) {
        is TranslationEngineArtwork.Glyph -> TranslationEngineGlyph(artwork, size, modifier)
        is TranslationEngineArtwork.Bundled,
        is TranslationEngineArtwork.InstalledApplication,
        -> ArtworkTile(size = size, modifier = modifier) {}
    }
}
