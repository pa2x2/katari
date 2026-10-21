package eu.kanade.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy
import dev.icerock.moko.resources.StringResource
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

// The status bar takes the colour of the topmost banner, so its icons are chosen against these fills.
val DownloadedOnlyBannerContainer = SurfaceContainer.Tertiary
val IncognitoModeBannerContainer = SurfaceContainer.Primary
val IndexingBannerContainer = SurfaceContainer.Secondary

@Composable
fun WarningBanner(
    textRes: StringResource,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth(), container = SurfaceContainer.WarningContainer) {
        Text(
            text = stringResource(textRes),
            modifier = Modifier
                .fillMaxWidth()
                .padding(KatariTheme.spacing.l),
            role = TextRole.Body,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun AppStateBanners(
    downloadedOnlyMode: Boolean,
    incognitoMode: Boolean,
    indexing: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val mainInsets = WindowInsets.statusBars
    val mainInsetsTop = mainInsets.getTop(density)
    SubcomposeLayout(modifier = modifier) { constraints ->
        val indexingPlaceable = subcompose(0) {
            AnimatedVisibility(
                visible = indexing,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                IndexingDownloadBanner(
                    modifier = Modifier.windowInsetsPadding(mainInsets),
                )
            }
        }.fastMap { it.measure(constraints) }
        val indexingHeight = indexingPlaceable.fastMaxBy { it.height }?.height ?: 0

        val downloadedOnlyPlaceable = subcompose(1) {
            AnimatedVisibility(
                visible = downloadedOnlyMode,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                val top = (mainInsetsTop - indexingHeight).coerceAtLeast(0)
                DownloadedOnlyModeBanner(
                    modifier = Modifier.windowInsetsPadding(WindowInsets(top = top)),
                )
            }
        }.fastMap { it.measure(constraints) }
        val downloadedOnlyHeight = downloadedOnlyPlaceable.fastMaxBy { it.height }?.height ?: 0

        val incognitoPlaceable = subcompose(2) {
            AnimatedVisibility(
                visible = incognitoMode,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                val top = (mainInsetsTop - indexingHeight - downloadedOnlyHeight).coerceAtLeast(0)
                IncognitoModeBanner(
                    modifier = Modifier.windowInsetsPadding(WindowInsets(top = top)),
                )
            }
        }.fastMap { it.measure(constraints) }
        val incognitoHeight = incognitoPlaceable.fastMaxBy { it.height }?.height ?: 0

        layout(constraints.maxWidth, indexingHeight + downloadedOnlyHeight + incognitoHeight) {
            indexingPlaceable.fastForEach {
                it.place(0, 0)
            }
            downloadedOnlyPlaceable.fastForEach {
                it.place(0, indexingHeight)
            }
            incognitoPlaceable.fastForEach {
                it.place(0, indexingHeight + downloadedOnlyHeight)
            }
        }
    }
}

@Composable
private fun DownloadedOnlyModeBanner(modifier: Modifier = Modifier) {
    ModeBanner(
        text = stringResource(MR.strings.label_downloaded_only),
        container = DownloadedOnlyBannerContainer,
        modifier = modifier,
    )
}

@Composable
private fun IncognitoModeBanner(modifier: Modifier = Modifier) {
    ModeBanner(
        text = stringResource(MR.strings.pref_incognito_mode),
        container = IncognitoModeBannerContainer,
        modifier = modifier,
    )
}

@Composable
private fun ModeBanner(text: String, container: SurfaceContainer, modifier: Modifier) {
    Surface(modifier = Modifier.fillMaxWidth(), container = container) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(KatariTheme.spacing.xs)
                .then(modifier),
            role = TextRole.Caption,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun IndexingDownloadBanner(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    Surface(modifier = Modifier.fillMaxWidth(), container = IndexingBannerContainer) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KatariTheme.spacing.s)
                .then(modifier),
            horizontalArrangement = Arrangement.Center,
        ) {
            var textHeight by remember { mutableStateOf(0.dp) }
            LoadingIndicator(
                modifier = Modifier.requiredSize(textHeight),
                tone = ContentTone.Inherit,
            )
            Spacer(modifier = Modifier.width(KatariTheme.spacing.s))
            Text(
                text = stringResource(MR.strings.download_notifier_cache_renewal),
                role = TextRole.Caption,
                textAlign = TextAlign.Center,
                onTextLayout = {
                    with(density) {
                        textHeight = it.size.height.toDp()
                    }
                },
            )
        }
    }
}
