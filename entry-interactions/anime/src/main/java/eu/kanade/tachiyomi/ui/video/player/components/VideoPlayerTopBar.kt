package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import mihon.entry.interactions.source.EntryChildWebViewAction
import mihon.entry.interactions.source.EntryChildWebViewActionsMenu
import mihon.entry.interactions.source.EntryChildWebViewResolution
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowBack
import mihon.icons.materialsymbols.rounded.Lock
import mihon.icons.materialsymbols.rounded.PictureInPictureAlt
import mihon.icons.materialsymbols.rounded.Settings
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun VideoPlayerTopBar(
    videoTitle: String,
    episodeName: String,
    onBack: () -> Unit,
    showPictureInPictureButton: Boolean,
    onEnterPictureInPicture: () -> Unit,
    onToggleLock: () -> Unit,
    onOpenSettings: () -> Unit,
    childWebView: EntryChildWebViewResolution.Available?,
    onChildWebViewAction: (EntryChildWebViewAction, EntryChildWebViewResolution.Available) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = KatariTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(videoPlayerTopShade())
            .videoPlayerSafeContentPadding(includeTop = true)
            .padding(spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            icon = MaterialSymbols.AutoMirroredRounded.ArrowBack,
            contentDescription = stringResource(MR.strings.action_back),
            onClick = onBack,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                icon = MaterialSymbols.Rounded.Lock,
                contentDescription = stringResource(MR.strings.anime_playback_lock_controls),
                onClick = onToggleLock,
                tone = ContentTone.OnMedia,
            )
            if (showPictureInPictureButton) {
                IconButton(
                    icon = MaterialSymbols.Rounded.PictureInPictureAlt,
                    contentDescription = stringResource(MR.strings.pref_enable_anime_picture_in_picture),
                    onClick = onEnterPictureInPicture,
                    tone = ContentTone.OnMedia,
                )
            }
            IconButton(
                icon = MaterialSymbols.Rounded.Settings,
                contentDescription = stringResource(MR.strings.label_settings),
                onClick = onOpenSettings,
                tone = ContentTone.OnMedia,
            )
            EntryChildWebViewActionsMenu(
                resolution = childWebView,
                onAction = onChildWebViewAction,
                tone = ContentTone.OnMedia,
            )
        }
    }
}
