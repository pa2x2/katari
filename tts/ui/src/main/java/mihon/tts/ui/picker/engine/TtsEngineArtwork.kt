package mihon.tts.ui.picker.engine

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.tts.api.engine.TtsEngineArtwork

@Composable
internal fun TtsEngineArtwork(
    artwork: TtsEngineArtwork,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val displayDensity = LocalDensity.current.density
    val iconPixels = (size.value * displayDensity).toInt().coerceAtLeast(1)
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
    val artworkPadding = when (artwork) {
        is TtsEngineArtwork.Bundled -> 10.dp
        is TtsEngineArtwork.InstalledApplication -> if (installedIcon == null) 10.dp else 6.dp
    }

    Box(
        modifier = modifier
            .size(size)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(size / 3),
            )
            .padding(artworkPadding),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap = installedIcon ?: fallbackIcon,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
