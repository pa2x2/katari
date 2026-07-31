package mihon.translation.ui.picker.engine

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.translation.api.engine.TranslationEngineArtwork

@Composable
internal fun TranslationEngineArtwork(
    artwork: TranslationEngineArtwork,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val displayDensity = LocalDensity.current.density
    val installedIcon by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = artwork,
        key2 = displayDensity,
    ) {
        value = when (artwork) {
            is TranslationEngineArtwork.Bundled -> null
            is TranslationEngineArtwork.InstalledApplication -> withContext(Dispatchers.IO) {
                runCatching {
                    val pixels = (size.value * displayDensity).toInt().coerceAtLeast(1)
                    context.packageManager
                        .getApplicationIcon(artwork.packageName)
                        .toBitmap(width = pixels, height = pixels)
                        .asImageBitmap()
                }.getOrNull()
            }
        }
    }
    val fallbackResource = when (artwork) {
        is TranslationEngineArtwork.Bundled -> artwork.resourceId
        is TranslationEngineArtwork.InstalledApplication -> artwork.fallbackResourceId
    }
    val artworkPadding = when (artwork) {
        is TranslationEngineArtwork.Bundled -> 10.dp
        is TranslationEngineArtwork.InstalledApplication -> if (installedIcon == null) 10.dp else 6.dp
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
        if (installedIcon != null) {
            Image(
                bitmap = installedIcon!!,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Image(
                painter = painterResource(fallbackResource),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
