package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.background
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
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.SwipeVertical
import mihon.icons.materialsymbols.rounded.TouchApp
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

/**
 * Labels the player's touch areas the first time it opens in a profile: the brightness and volume halves with their
 * double-tap seeks, and hold for 2× in the middle. Any tap dismisses it.
 */
@Composable
internal fun VideoPlayerGestureGuide(
    seekStepSeconds: Int,
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
            GestureHint(
                icon = MaterialSymbols.Rounded.SwipeVertical,
                title = stringResource(MR.strings.anime_guide_brightness),
                detail = stringResource(MR.strings.anime_guide_double_tap_back, seekStepSeconds),
                modifier = Modifier.weight(1f),
            )
            GestureHint(
                icon = MaterialSymbols.Rounded.TouchApp,
                title = stringResource(MR.strings.anime_guide_hold),
                detail = stringResource(MR.strings.anime_guide_tap),
                modifier = Modifier.weight(1f),
            )
            GestureHint(
                icon = MaterialSymbols.Rounded.SwipeVertical,
                title = stringResource(MR.strings.anime_guide_volume),
                detail = stringResource(MR.strings.anime_guide_double_tap_forward, seekStepSeconds),
                modifier = Modifier.weight(1f),
            )
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
private fun GestureHint(
    icon: ImageVector,
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(KatariTheme.sizes.iconLarge),
            tone = ContentTone.OnMedia,
        )
        Text(text = title, role = TextRole.SectionTitle, tone = ContentTone.OnMedia, textAlign = TextAlign.Center)
        Text(text = detail, role = TextRole.Supporting, tone = ContentTone.OnMediaVariant, textAlign = TextAlign.Center)
    }
}
