package eu.kanade.presentation.browse.components

import android.util.DisplayMetrics
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import eu.kanade.presentation.util.rememberResourceBitmapPainter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.coil.InstalledExtensionIcon
import eu.kanade.tachiyomi.data.coil.InstalledSourceIcon
import eu.kanade.tachiyomi.extension.model.Extension
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.roundedfilled.Dangerous
import mihon.icons.materialsymbols.roundedfilled.Warning
import mihon.presentation.kit.content.BrowseMetrics
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.domain.source.model.Source
import tachiyomi.source.local.isLocal

private val defaultModifier = Modifier
    .height(BrowseMetrics.sourceIcon)
    .aspectRatio(1f)

@Composable
fun SourceIcon(
    source: Source,
    modifier: Modifier = Modifier,
) {
    val iconModifier = modifier.then(defaultModifier)

    when {
        source.isStub -> {
            Icon(
                imageVector = MaterialSymbols.RoundedFilled.Warning,
                contentDescription = null,
                modifier = iconModifier,
                tone = ContentTone.Error,
            )
        }
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
                model = InstalledSourceIcon(source.id),
                contentDescription = null,
                placeholder = defaultIcon,
                error = defaultIcon,
                modifier = iconModifier,
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
                model = extension.iconUrl,
                contentDescription = null,
                placeholder = ColorPainter(KatariTheme.colors.placeholder),
                error = rememberResourceBitmapPainter(id = R.drawable.cover_error),
                modifier = modifier
                    .clip(KatariTheme.shapes.extraSmall),
            )
        }
        is Extension.Loaded -> {
            val defaultIcon = painterResource(R.mipmap.ic_default_source)
            AsyncImage(
                model = InstalledExtensionIcon(extension.pkgName, extension.versionCode, density),
                contentDescription = null,
                placeholder = defaultIcon,
                error = defaultIcon,
                modifier = modifier,
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
