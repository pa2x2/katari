package app.katari.translation.ui.picker.engine

import androidx.annotation.DrawableRes
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
import androidx.core.graphics.drawable.toBitmap
import app.katari.presentation.kit.surface.ArtworkTile
import app.katari.presentation.kit.surface.ArtworkTileSize
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.translation.api.engine.TranslationEngineArtwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The installed application's icon when there is one, otherwise the bundled glyph. A glyph gets a wider inset than an
 * application icon, which already carries its own padding.
 */
@Composable
internal actual fun TranslationEngineArtwork(
    artwork: TranslationEngineArtwork,
    size: ArtworkTileSize,
    modifier: Modifier,
) {
    when (artwork) {
        is TranslationEngineArtwork.Glyph -> TranslationEngineGlyph(artwork, size, modifier)
        is TranslationEngineArtwork.Bundled -> AndroidArtwork(null, artwork.resourceId, size, modifier)
        is TranslationEngineArtwork.InstalledApplication ->
            AndroidArtwork(artwork.packageName, artwork.fallbackResourceId, size, modifier)
    }
}

/** The icon of the application [packageName] where it is installed, otherwise the drawable [fallbackResource]. */
@Composable
private fun AndroidArtwork(
    packageName: String?,
    @DrawableRes fallbackResource: Int,
    size: ArtworkTileSize,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val displayDensity = LocalDensity.current.density
    val installedIcon by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = packageName,
        key2 = displayDensity,
    ) {
        value = packageName?.let {
            withContext(Dispatchers.IO) {
                runCatching {
                    val pixels = (size.side.value * displayDensity).toInt().coerceAtLeast(1)
                    context.packageManager
                        .getApplicationIcon(packageName)
                        .toBitmap(width = pixels, height = pixels)
                        .asImageBitmap()
                }.getOrNull()
            }
        }
    }
    val inset = if (installedIcon == null) KatariTheme.spacing.m else KatariTheme.spacing.s
    val imageModifier = Modifier
        .fillMaxSize()
        .padding(inset)

    ArtworkTile(size = size, modifier = modifier) {
        val icon = installedIcon
        if (icon != null) {
            Image(bitmap = icon, contentDescription = null, modifier = imageModifier)
        } else {
            Image(painter = painterResource(fallbackResource), contentDescription = null, modifier = imageModifier)
        }
    }
}
