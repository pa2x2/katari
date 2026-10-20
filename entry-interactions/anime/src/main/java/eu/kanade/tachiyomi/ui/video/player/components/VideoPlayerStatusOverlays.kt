package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowBack
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.surface.MediaBackdrop
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The wait before an episode's first frame; the heading shows once the title is known, so there is a way back. */
@Composable
internal fun VideoPlayerLoadingOverlay(
    videoTitle: String?,
    episodeName: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        MediaBackdrop(modifier = Modifier.matchParentSize())
        LoadingIndicator(color = KatariTheme.colors.onMedia)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .videoPlayerSafeContentPadding(includeTop = true)
                .padding(KatariTheme.spacing.s),
        ) {
            VideoPlayerHeading(
                videoTitle = videoTitle.orEmpty(),
                episodeName = episodeName.orEmpty(),
                onBack = onBack,
            )
        }
    }
}

/**
 * Playback that could not start or stopped with an error, over the media background. [onCancel] and [onSettings] add
 * a secondary action beside Retry.
 */
@Composable
internal fun VideoPlayerErrorOverlay(
    title: String,
    message: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null,
) {
    val spacing = KatariTheme.spacing
    Box(
        modifier = modifier.background(KatariTheme.colors.media),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(spacing.xl),
        ) {
            Text(text = title, role = TextRole.SheetTitle, tone = ContentTone.OnMedia)
            Text(
                text = message,
                modifier = Modifier.padding(top = spacing.m),
                role = TextRole.Body,
                tone = ContentTone.OnMediaVariant,
            )
            if (onRetry != null || onCancel != null || onSettings != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.m),
                    modifier = Modifier.padding(top = spacing.xl),
                ) {
                    onCancel?.let {
                        Button(
                            label = stringResource(MR.strings.action_cancel),
                            onClick = it,
                            style = ButtonStyle.Tonal,
                        )
                    }
                    onSettings?.let {
                        Button(
                            label = stringResource(MR.strings.label_settings),
                            onClick = it,
                            style = ButtonStyle.Tonal,
                        )
                    }
                    onRetry?.let {
                        Button(label = stringResource(MR.strings.action_retry), onClick = it)
                    }
                }
            }
            IconButton(
                icon = MaterialSymbols.AutoMirroredRounded.ArrowBack,
                contentDescription = stringResource(MR.strings.action_back),
                onClick = onBack,
                modifier = Modifier.padding(top = spacing.l),
                tone = ContentTone.OnMedia,
            )
        }
    }
}

/** A pill under the top bar while the stream or episode changes behind the one still playing. */
@Composable
internal fun VideoPlayerSwitchingOverlay(
    label: String,
    modifier: Modifier = Modifier,
) {
    val spacing = KatariTheme.spacing
    Surface(
        modifier = modifier
            .videoPlayerSafeContentPadding(includeTop = true)
            .padding(top = spacing.xl),
        container = SurfaceContainer.MediaScrim,
        shape = KatariTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.l, vertical = spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.m),
        ) {
            LoadingIndicator(
                modifier = Modifier.size(KatariTheme.sizes.iconSmall),
                color = KatariTheme.colors.onMedia,
            )
            Text(text = label, role = TextRole.Body)
        }
    }
}
