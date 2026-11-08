package mihon.tts.ui.picker.engine

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.surface.ArtworkTileSize
import mihon.tts.api.engine.TtsEngineArtwork

/** An engine's logo on a rounded tile. */
@Composable
internal expect fun TtsEngineArtwork(
    artwork: TtsEngineArtwork,
    size: ArtworkTileSize,
    modifier: Modifier = Modifier,
)
