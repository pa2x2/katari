package eu.kanade.presentation.browse.components

import android.util.DisplayMetrics
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations
import eu.kanade.presentation.util.rememberResourceBitmapPainter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.coil.ExtensionIconTrim
import eu.kanade.tachiyomi.data.coil.InstalledExtensionIcon
import eu.kanade.tachiyomi.data.coil.InstalledSourceIcon
import eu.kanade.tachiyomi.extension.model.Extension
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.roundedfilled.Dangerous
import mihon.icons.materialsymbols.roundedfilled.Warning
import mihon.presentation.kit.content.BrowseMetrics
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.surface.LetterTile
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.domain.source.model.Source
import tachiyomi.source.local.isLocal

private val defaultModifier = Modifier
    .height(BrowseMetrics.sourceIcon)
    .aspectRatio(1f)

/**
 * Icons ship in whatever shape their extension drew; one mask lines them up. Their own margin is trimmed away first
 * ([ExtensionIconTrim]) so it doesn't show as a second frame inside the mask.
 */
@Composable
private fun Modifier.iconMask(): Modifier = clip(
    KatariTheme.shapes.small,
).background(KatariTheme.colors.surfaceContainer)

@Composable
fun SourceIcon(
    source: Source,
    modifier: Modifier = Modifier,
) {
    val iconModifier = modifier.then(defaultModifier)

    when {
        // Its row already says it isn't installed; the letter keeps the rows in a list of them apart
        source.isStub -> LetterTile(name = source.name.ifBlank { source.id.toString() }, modifier = iconModifier)
        source.isLocal() -> {
            Image(
                painter = painterResource(R.mipmap.ic_local_source),
                contentDescription = null,
                modifier = iconModifier,
            )
        }
        else -> {
            val defaultIcon = painterResource(R.mipmap.ic_default_source)
            AsyncImage(
                model = rememberTrimmedIconRequest(InstalledSourceIcon(source.id)),
                contentDescription = null,
                placeholder = defaultIcon,
                error = defaultIcon,
                modifier = iconModifier.iconMask(),
            )
        }
    }
}

@Composable
fun ExtensionIcon(
    extension: Extension,
    modifier: Modifier = Modifier,
    density: Int = DisplayMetrics.DENSITY_DEFAULT,
) {
    when (extension) {
        is Extension.Available -> {
            AsyncImage(
                model = rememberTrimmedIconRequest(extension.iconUrl),
                contentDescription = null,
                placeholder = ColorPainter(KatariTheme.colors.placeholder),
                error = rememberResourceBitmapPainter(id = R.drawable.cover_error),
                modifier = modifier.iconMask(),
            )
        }
        is Extension.Loaded -> {
            val defaultIcon = painterResource(R.mipmap.ic_default_source)
            AsyncImage(
                model = rememberTrimmedIconRequest(
                    InstalledExtensionIcon(extension.pkgName, extension.versionCode, density),
                ),
                contentDescription = null,
                placeholder = defaultIcon,
                error = defaultIcon,
                modifier = modifier.iconMask(),
            )
        }
        is Extension.NotLoaded -> Icon(
            imageVector = MaterialSymbols.RoundedFilled.Dangerous,
            contentDescription = null,
            modifier = modifier.then(defaultModifier),
            tone = ContentTone.Error,
        )
    }
}

@Composable
private fun rememberTrimmedIconRequest(data: Any?): ImageRequest {
    val context = LocalContext.current
    return remember(context, data) {
        ImageRequest.Builder(context)
            .data(data)
            .transformations(ExtensionIconTrim())
            .build()
    }
}
