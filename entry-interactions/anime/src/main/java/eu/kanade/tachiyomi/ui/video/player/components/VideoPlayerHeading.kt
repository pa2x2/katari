package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowBack
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.button.IconButtonSize
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The title and episode a screen of the player names under its back button. */
internal data class VideoPlayerHeadingText(val videoTitle: String, val episodeName: String)

/** Back and the title and episode being watched: the start of the top bar, and all of it while an episode loads. */
@Composable
internal fun VideoPlayerHeading(
    videoTitle: String,
    episodeName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = KatariTheme.spacing
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            icon = MaterialSymbols.AutoMirroredRounded.ArrowBack,
            contentDescription = stringResource(MR.strings.action_back),
            onClick = onBack,
            size = IconButtonSize.Large,
            tone = ContentTone.OnMedia,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = spacing.s, end = spacing.m),
        ) {
            Text(
                text = videoTitle,
                role = TextRole.SectionTitle,
                tone = ContentTone.OnMedia,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = episodeName,
                role = TextRole.Supporting,
                tone = ContentTone.OnMediaVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
