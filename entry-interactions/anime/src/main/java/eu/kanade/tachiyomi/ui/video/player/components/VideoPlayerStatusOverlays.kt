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
import androidx.compose.ui.graphics.vector.ImageVector
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.roundedfilled.FastForward
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.surface.OverlayPill
import mihon.presentation.kit.surface.OverlayStyle
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** A pill under the top bar while the stream or episode changes behind the one still playing. */
@Composable
internal fun VideoPlayerSwitchingOverlay(
    label: String,
    modifier: Modifier = Modifier,
) {
    VideoPlayerStatusPill(
        label = label,
        modifier = modifier,
        leading = {
            LoadingIndicator(
                modifier = Modifier.size(KatariTheme.sizes.iconSmall),
                tone = ContentTone.OnMedia,
            )
        },
    )
}

/** The speed playing while the screen is held, which nothing else on screen shows. */
@Composable
internal fun VideoPlayerSpeedBoostPill(
    speedLabel: String,
    modifier: Modifier = Modifier,
) {
    VideoPlayerStatusPill(
        label = speedLabel,
        modifier = modifier,
        leading = {
            Icon(
                imageVector = MaterialSymbols.RoundedFilled.FastForward,
                contentDescription = null,
                modifier = Modifier.size(KatariTheme.sizes.iconSmall),
            )
        },
    )
}

/** How the picture is framed while a pinch zooms it, which shows what the fingers will leave once they lift. */
@Composable
internal fun VideoPlayerPicturePill(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    VideoPlayerStatusPill(
        label = label,
        modifier = modifier,
        leading = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(KatariTheme.sizes.iconSmall),
            )
        },
    )
}

/**
 * A word about the player itself, such as the engine Auto moved to, with an optional action and a way to dismiss it.
 * The caller decides how long it stays.
 */
@Composable
internal fun VideoPlayerNoticePill(
    label: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    onDismiss: (() -> Unit)? = null,
) {
    VideoPlayerStatusPill(
        label = label,
        modifier = modifier,
        trailing = if (actionLabel != null || onDismiss != null) {
            {
                actionLabel?.let { Button(label = it, onClick = onAction, style = ButtonStyle.Text) }
                onDismiss?.let {
                    IconButton(
                        icon = MaterialSymbols.Rounded.Close,
                        contentDescription = stringResource(MR.strings.action_close),
                        onClick = it,
                    )
                }
            }
        } else {
            null
        },
    )
}

@Composable
private fun VideoPlayerStatusPill(
    label: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val spacing = KatariTheme.spacing
    OverlayPill(
        modifier = modifier
            .videoPlayerSafeContentPadding(includeTop = true)
            .padding(top = spacing.xl, start = spacing.l, end = spacing.l),
        style = OverlayStyle.Scrim,
    ) {
        Row(
            modifier = Modifier.padding(
                start = spacing.l,
                end = if (trailing != null) spacing.xs else spacing.l,
                top = spacing.s,
                bottom = spacing.s,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.m),
        ) {
            leading?.invoke()
            Text(text = label, role = TextRole.Body, modifier = Modifier.weight(1f, fill = false))
            trailing?.let { Row(verticalAlignment = Alignment.CenterVertically) { it() } }
        }
    }
}
