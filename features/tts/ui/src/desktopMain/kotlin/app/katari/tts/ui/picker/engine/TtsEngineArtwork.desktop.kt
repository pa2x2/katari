package app.katari.tts.ui.picker.engine

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.surface.ArtworkTile
import app.katari.presentation.kit.surface.ArtworkTileSize
import app.katari.tts.api.engine.TtsEngineArtwork

/**
 * The glyph, or an empty tile for artwork that names Android drawables and application packages, which only engines
 * of Android's own have.
 */
@Composable
internal actual fun TtsEngineArtwork(
    artwork: TtsEngineArtwork,
    size: ArtworkTileSize,
    modifier: Modifier,
) {
    when (artwork) {
        is TtsEngineArtwork.Glyph -> TtsEngineGlyph(artwork, size, modifier)
        is TtsEngineArtwork.Bundled,
        is TtsEngineArtwork.InstalledApplication,
        -> ArtworkTile(size = size, modifier = modifier) {}
    }
}
