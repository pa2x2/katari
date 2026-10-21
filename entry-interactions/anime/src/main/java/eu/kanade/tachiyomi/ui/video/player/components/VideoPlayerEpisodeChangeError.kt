package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Error
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.Card
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Why another episode couldn't be opened, shown over the episode that stays on screen. */
@Composable
internal fun VideoPlayerEpisodeChangeError(
    episodeName: String,
    message: String,
    onDismiss: () -> Unit,
    onOpenWebView: (() -> Unit)?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = KatariTheme.spacing
    Card(modifier = modifier.widthIn(max = KatariTheme.sizes.readableWidth)) {
        Row(
            modifier = Modifier.padding(start = spacing.l, top = spacing.l, end = spacing.l),
            horizontalArrangement = Arrangement.spacedBy(spacing.m),
        ) {
            Icon(imageVector = MaterialSymbols.Rounded.Error, contentDescription = null, tone = ContentTone.Error)
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                Text(
                    text = stringResource(MR.strings.anime_playback_open_episode_failed, episodeName),
                    role = TextRole.SectionTitle,
                )
                Text(text = message, role = TextRole.Supporting)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s, vertical = spacing.s),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs, Alignment.End),
        ) {
            Button(label = stringResource(MR.strings.action_close), onClick = onDismiss, style = ButtonStyle.Text)
            onOpenWebView?.let {
                Button(
                    label = stringResource(MR.strings.action_open_in_web_view),
                    onClick = it,
                    style = ButtonStyle.Text,
                )
            }
            Button(label = stringResource(MR.strings.action_retry), onClick = onRetry)
        }
    }
}
