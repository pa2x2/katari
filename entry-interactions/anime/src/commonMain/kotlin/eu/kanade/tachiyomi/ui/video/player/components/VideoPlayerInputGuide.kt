package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.surface.background
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** One column of [VideoPlayerInputGuide]: what to do, and what else works there. */
internal data class VideoPlayerGuideHint(
    val icon: ImageVector,
    val title: String,
    val details: List<String>,
)

/**
 * What the player's input does, shown over the picture the first time it opens in a profile: [hints] side by side, and
 * Got it under them. Any click or tap dismisses it.
 */
@Composable
internal fun VideoPlayerInputGuide(
    hints: List<VideoPlayerGuideHint>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = KatariTheme.spacing
    Box(
        modifier = modifier
            .background(SurfaceContainer.MediaShade)
            .clickable(
                interactionSource = remember {
                    MutableInteractionSource()
                },
                indication = null,
                onClick = onDismiss,
            )
            .videoPlayerSafeContentPadding(includeTop = true, includeBottom = true),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = spacing.l),
            horizontalArrangement = Arrangement.spacedBy(spacing.l),
        ) {
            hints.forEach { hint -> GuideHint(hint, Modifier.weight(1f)) }
        }
        Button(
            label = stringResource(MR.strings.action_got_it),
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = spacing.xl),
            style = ButtonStyle.Tonal,
        )
    }
}

@Composable
private fun GuideHint(hint: VideoPlayerGuideHint, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
    ) {
        Icon(
            imageVector = hint.icon,
            contentDescription = null,
            modifier = Modifier.size(KatariTheme.sizes.iconLarge),
            tone = ContentTone.OnMedia,
        )
        Text(text = hint.title, role = TextRole.SectionTitle, tone = ContentTone.OnMedia, textAlign = TextAlign.Center)
        hint.details.forEach { detail ->
            Text(
                text = detail,
                role = TextRole.Supporting,
                tone = ContentTone.OnMediaVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
