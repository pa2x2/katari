package mihon.translation.ui.picker.engine

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.surface.ArtworkTile
import mihon.presentation.kit.surface.ArtworkTileSize
import mihon.translation.api.engine.TranslationEngineArtwork

/**
 * An empty tile: engine artwork names Android drawables and application packages, which desktop can't load. Desktop
 * has no translation engine yet, so no engine card shows one.
 */
@Composable
internal actual fun TranslationEngineArtwork(
    artwork: TranslationEngineArtwork,
    size: ArtworkTileSize,
    modifier: Modifier,
) {
    ArtworkTile(size = size, modifier = modifier) {}
}
