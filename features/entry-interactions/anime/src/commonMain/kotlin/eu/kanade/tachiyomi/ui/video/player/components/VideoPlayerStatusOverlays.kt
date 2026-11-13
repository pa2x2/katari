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
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Close
import app.katari.icons.materialsymbols.rounded.Error
import app.katari.icons.materialsymbols.rounded.Subtitles
import app.katari.icons.materialsymbols.roundedfilled.FastForward
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.button.IconButton
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.progress.LoadingIndicator
import app.katari.presentation.kit.surface.OverlayPill
import app.katari.presentation.kit.surface.OverlayStyle
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A pill under the top bar while the stream or episode changes behind the one still playing. [onCancel] stops an
 * episode change, the only way past it: until it plays or fails, nothing else asks for another episode.
 */
@Composable
internal fun VideoPlayerSwitchingOverlay(
    label: String,
    modifier: Modifier = Modifier,
    onCancel: (() -> Unit)? = null,
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
        trailing = onCancel?.let {
            {
                IconButton(
                    icon = MaterialSymbols.Rounded.Close,
                    contentDescription = stringResource(MR.strings.action_cancel),
                    onClick = it,
                )
            }
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

/** How the picture is framed while a gesture or key zooms or moves it, so the framing can be read without controls. */
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
 * A word about the player itself, such as the engine Auto moved to, with up to two actions and a way to dismiss it;
 * [failure] marks one about something that didn't work. The caller decides how long it stays.
 */
@Composable
internal fun VideoPlayerNoticePill(
    label: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    secondActionLabel: String? = null,
    onSecondAction: () -> Unit = {},
    onDismiss: (() -> Unit)? = null,
    failure: Boolean = false,
) {
    VideoPlayerStatusPill(
        label = label,
        modifier = modifier,
        leading = if (failure) {
            {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Error,
                    contentDescription = null,
                    modifier = Modifier.size(KatariTheme.sizes.iconSmall),
                    tone = ContentTone.Error,
                )
            }
        } else {
            null
        },
        trailing = if (actionLabel != null || onDismiss != null) {
            {
                actionLabel?.let { Button(label = it, onClick = onAction, style = ButtonStyle.Text) }
                secondActionLabel?.let { Button(label = it, onClick = onSecondAction, style = ButtonStyle.Text) }
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

/** The subtitle timing set by a key, which nothing else on screen shows while the timing bar is closed. */
@Composable
internal fun VideoPlayerSubtitleTimingPill(
    label: String,
    modifier: Modifier = Modifier,
) {
    VideoPlayerStatusPill(
        label = label,
        modifier = modifier,
        leading = {
            Icon(
                imageVector = MaterialSymbols.Rounded.Subtitles,
                contentDescription = null,
                modifier = Modifier.size(KatariTheme.sizes.iconSmall),
            )
        },
    )
}

/**
 * Where what stays over the video while the controls come and go shows, such as the subtitle timing bar or a failed
 * episode change: under the top bar, so neither covers the other at any width. The status pills, which come and go,
 * keep to the top.
 */
@Composable
internal fun Modifier.videoPlayerUnderTopBarSlot(): Modifier {
    val spacing = KatariTheme.spacing
    return videoPlayerSafeContentPadding(includeTop = true)
        .padding(top = KatariTheme.sizes.touchTarget + spacing.xl, start = spacing.l, end = spacing.l)
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
