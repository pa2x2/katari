package mihon.translation.ui.picker.engine

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import mihon.translation.api.engine.TranslationEngineArtwork

/**
 * An engine's logo on a rounded tile: the installed application's icon when there is one, otherwise the bundled
 * glyph. A glyph gets a wider inset than an application icon, which already carries its own padding.
 */
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
    val inset = if (installedIcon == null) KatariTheme.spacing.m else KatariTheme.spacing.s
    val imageModifier = Modifier
        .fillMaxSize()
        .padding(inset)

    Surface(
        modifier = modifier.size(size),
        container = SurfaceContainer.Highest,
        shape = KatariTheme.shapes.large,
    ) {
        val icon = installedIcon
        if (icon != null) {
            Image(bitmap = icon, contentDescription = null, modifier = imageModifier)
        } else {
            Image(painter = painterResource(fallbackResource), contentDescription = null, modifier = imageModifier)
        }
    }
}
