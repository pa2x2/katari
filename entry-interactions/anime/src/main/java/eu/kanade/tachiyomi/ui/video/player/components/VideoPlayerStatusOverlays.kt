package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.roundedfilled.FastForward
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.surface.MediaBackdrop
import mihon.presentation.kit.surface.OverlayPill
import mihon.presentation.kit.surface.OverlayStyle
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

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
        LoadingIndicator(tone = ContentTone.OnMedia)
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

/** A pill under the top bar while the stream or episode changes behind the one still playing. */
@Composable
internal fun VideoPlayerSwitchingOverlay(
    label: String,
    modifier: Modifier = Modifier,
) {
    VideoPlayerStatusPill(label = label, modifier = modifier) {
        LoadingIndicator(
            modifier = Modifier.size(KatariTheme.sizes.iconSmall),
            tone = ContentTone.OnMedia,
        )
    }
}

/** The speed playing while the screen is held, which nothing else on screen shows. */
@Composable
internal fun VideoPlayerSpeedBoostPill(
    speedLabel: String,
    modifier: Modifier = Modifier,
) {
    VideoPlayerStatusPill(label = speedLabel, modifier = modifier) {
        Icon(
            imageVector = MaterialSymbols.RoundedFilled.FastForward,
            contentDescription = null,
            modifier = Modifier.size(KatariTheme.sizes.iconSmall),
        )
    }
}

@Composable
private fun VideoPlayerStatusPill(
    label: String,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit,
) {
    val spacing = KatariTheme.spacing
    OverlayPill(
        modifier = modifier
            .videoPlayerSafeContentPadding(includeTop = true)
            .padding(top = spacing.xl),
        style = OverlayStyle.Scrim,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.l, vertical = spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.m),
        ) {
            leading()
            Text(text = label, role = TextRole.Body)
        }
    }
}
