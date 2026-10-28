package mihon.translation.ui.picker.engine

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.surface.ArtworkTileSize
import mihon.translation.api.engine.TranslationEngineArtwork

/** An engine's logo on a rounded tile. */
@Composable
internal expect fun TranslationEngineArtwork(
    artwork: TranslationEngineArtwork,
    size: ArtworkTileSize,
    modifier: Modifier = Modifier,
)
