package mihon.tts.ui.picker.engine

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.presentation.kit.surface.ArtworkTile
import mihon.presentation.kit.surface.ArtworkTileSize
import mihon.presentation.kit.theme.KatariTheme
import mihon.tts.api.engine.TtsEngineArtwork

/**
 * An engine's logo on a rounded tile: the installed application's icon when there is one, otherwise the bundled
 * glyph. A glyph gets a wider inset than an application icon, which already carries its own padding.
 */
@Composable
internal fun TtsEngineArtwork(
    artwork: TtsEngineArtwork,
    size: ArtworkTileSize,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val displayDensity = LocalDensity.current.density
    val iconPixels = (size.side.value * displayDensity).toInt().coerceAtLeast(1)
    val fallbackResource = when (artwork) {
        is TtsEngineArtwork.Bundled -> artwork.resourceId
        is TtsEngineArtwork.InstalledApplication -> artwork.fallbackResourceId
    }
    val fallbackIcon = remember(context, fallbackResource, iconPixels) {
        val drawable = checkNotNull(ContextCompat.getDrawable(context, fallbackResource)) {
            "TTS engine artwork resource $fallbackResource could not be loaded"
        }
        drawable.toBitmap(width = iconPixels, height = iconPixels).asImageBitmap()
    }
    val installedIcon by produceState<ImageBitmap?>(null, artwork, iconPixels) {
        value = when (artwork) {
            is TtsEngineArtwork.Bundled -> null
            is TtsEngineArtwork.InstalledApplication -> withContext(Dispatchers.IO) {
                runCatching {
                    context.packageManager
                        .getApplicationIcon(artwork.packageName)
                        .toBitmap(width = iconPixels, height = iconPixels)
                        .asImageBitmap()
                }.getOrNull()
            }
        }
    }
    val inset = if (installedIcon == null) KatariTheme.spacing.m else KatariTheme.spacing.s

    ArtworkTile(size = size, modifier = modifier) {
        Image(
            bitmap = installedIcon ?: fallbackIcon,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(inset),
        )
    }
}
