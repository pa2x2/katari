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
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.surface.MediaBackdrop
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
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
