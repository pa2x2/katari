package eu.kanade.presentation.browse.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.roundedfilled.Dangerous
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.content.LogoFrame
import app.katari.presentation.kit.content.LogoSize
import app.katari.presentation.kit.content.rememberArtworkPlaceholder
import app.katari.presentation.kit.surface.LetterTile
import app.katari.presentation.kit.theme.ContentTone
import app.katari.ui.resources.Res
import app.katari.ui.resources.cover_error
import app.katari.ui.resources.ic_default_source
import app.katari.ui.resources.ic_local_source
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.transformations
import eu.kanade.tachiyomi.data.coil.ExtensionIconDensity
import eu.kanade.tachiyomi.data.coil.ExtensionIconTrim
import eu.kanade.tachiyomi.data.coil.InstalledExtensionIcon
import eu.kanade.tachiyomi.data.coil.InstalledSourceIcon
import eu.kanade.tachiyomi.extension.model.Extension
import org.jetbrains.compose.resources.painterResource
import tachiyomi.domain.source.identity.LOCAL_SOURCE_ID
import tachiyomi.domain.source.model.Source

/**
 * Source and extension icons, each on the kit's logo tile. Icons ship with whatever margin their extension drew around
 * them; it is trimmed away first ([ExtensionIconTrim]) so it doesn't show as a second frame inside the tile.
 */
@Composable
fun SourceIcon(
    source: Source,
    modifier: Modifier = Modifier,
    size: LogoSize = LogoSize.Row,
) {
    LogoFrame(size = size, modifier = modifier) {
        when {
            // Its row already says it isn't installed; the letter keeps the rows in a list of them apart
            source.isStub -> LetterTile(
                name = source.name.ifBlank { source.id.toString() },
                modifier = Modifier.fillMaxSize(),
            )
            source.id == LOCAL_SOURCE_ID -> Image(
                painter = painterResource(Res.drawable.ic_local_source),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
            else -> InstalledIcon(InstalledSourceIcon(source.id))
        }
    }
}

/** The icon of the installed extension that provides the source with [sourceId], for a list that only has its id. */
@Composable
fun SourceIcon(
    sourceId: Long,
    modifier: Modifier = Modifier,
    size: LogoSize = LogoSize.Row,
) {
    LogoFrame(size = size, modifier = modifier) {
        InstalledIcon(InstalledSourceIcon(sourceId))
    }
}

@Composable
fun ExtensionIcon(
    extension: Extension,
    modifier: Modifier = Modifier,
    size: LogoSize = LogoSize.Row,
    density: ExtensionIconDensity = ExtensionIconDensity.Baseline,
) {
    LogoFrame(size = size, modifier = modifier) {
        when (extension) {
            is Extension.Available -> AsyncImage(
                model = rememberTrimmedIconRequest(extension.iconUrl),
                contentDescription = null,
                placeholder = rememberArtworkPlaceholder(),
                error = painterResource(Res.drawable.cover_error),
                modifier = Modifier.fillMaxSize(),
            )
            is Extension.Loaded -> InstalledIcon(
                InstalledExtensionIcon(extension.pkgName, extension.form, extension.versionCode, density),
            )
            is Extension.NotLoaded -> Icon(
                imageVector = MaterialSymbols.RoundedFilled.Dangerous,
                contentDescription = null,
                tone = ContentTone.Error,
            )
        }
    }
}

/** The icon an installed package draws, or the default source icon while it loads or when it has none. */
@Composable
private fun InstalledIcon(data: Any) {
    val defaultIcon = painterResource(Res.drawable.ic_default_source)
    AsyncImage(
        model = rememberTrimmedIconRequest(data),
        contentDescription = null,
        placeholder = defaultIcon,
        error = defaultIcon,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun rememberTrimmedIconRequest(data: Any?): ImageRequest {
    val context = LocalPlatformContext.current
    return remember(context, data) {
        ImageRequest.Builder(context)
            .data(data)
            .transformations(ExtensionIconTrim())
            .build()
    }
}
