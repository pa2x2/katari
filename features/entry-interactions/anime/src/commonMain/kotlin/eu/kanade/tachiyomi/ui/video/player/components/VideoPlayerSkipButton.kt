package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import app.katari.entry.interactions.anime.introtimes.IntroTimesService
import app.katari.entry.viewer.chrome.ui.pointer.mediaControl
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.roundedfilled.SkipNext
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.shortcut.LocalKeyboardShortcuts
import app.katari.presentation.kit.surface.OverlayCountdownPill
import app.katari.presentation.kit.surface.OverlayPill
import app.katari.presentation.kit.surface.OverlayStyle
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.source.entry.PlaybackMarkerKind
import eu.kanade.tachiyomi.ui.video.player.introtimes.label
import eu.kanade.tachiyomi.ui.video.player.markers.VideoPlayerSkipCountdown
import eu.kanade.tachiyomi.ui.video.player.markers.VideoPlayerSkipOffer
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Skips the intro, recap, credits or preview playing, or moves on to the next episode when only those are left. With a
 * [countdown] the button fills before skipping by itself and can be stopped.
 */
@Composable
internal fun VideoPlayerSkipButton(
    offer: VideoPlayerSkipOffer,
    service: IntroTimesService?,
    countdown: VideoPlayerSkipCountdown?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Where keys work the player, the button takes focus when it shows, so Enter presses it until focus moves on
    val focus = remember { FocusRequester() }
    val takesFocus = LocalKeyboardShortcuts.current != null
    LaunchedEffect(takesFocus) { if (takesFocus) focus.requestFocus() }
    val pill = (if (takesFocus) modifier.focusRequester(focus) else modifier).mediaControl()
    if (countdown == null) {
        OverlayPill(modifier = pill, style = OverlayStyle.Raised, onClick = onClick) {
            SkipLabel(offer, service)
        }
    } else {
        OverlayCountdownPill(
            progress = countdown.progress,
            onClick = onClick,
            onDismiss = countdown.onDismiss,
            dismissLabel = stringResource(MR.strings.anime_skip_dont_skip),
            modifier = pill,
        ) {
            SkipLabel(offer, service)
        }
    }
}

@Composable
private fun SkipLabel(offer: VideoPlayerSkipOffer, service: IntroTimesService?) {
    val spacing = KatariTheme.spacing
    Row(
        modifier = Modifier.padding(horizontal = spacing.l, vertical = spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
    ) {
        Icon(
            imageVector = MaterialSymbols.RoundedFilled.SkipNext,
            contentDescription = null,
            modifier = Modifier.size(KatariTheme.sizes.iconSmall),
        )
        val label = stringResource(
            if (offer.goesToNextEpisode) {
                MR.strings.anime_playback_next_episode
            } else {
                offer.marker.kind.skipLabel
            },
        )
        Text(
            text = service?.let { stringResource(MR.strings.anime_skip_with_service, label, stringResource(it.label)) }
                ?: label,
            role = TextRole.Label,
            maxLines = 1,
        )
    }
}

private val PlaybackMarkerKind.skipLabel: StringResource
    get() = when (this) {
        PlaybackMarkerKind.INTRO, PlaybackMarkerKind.MIXED_INTRO -> MR.strings.anime_skip_intro
        PlaybackMarkerKind.RECAP -> MR.strings.anime_skip_recap
        PlaybackMarkerKind.OUTRO, PlaybackMarkerKind.MIXED_OUTRO -> MR.strings.anime_skip_credits
        PlaybackMarkerKind.PREVIEW -> MR.strings.anime_skip_preview
        PlaybackMarkerKind.CHAPTER -> error("A chapter is never offered for skipping")
    }

/** The notice after an automatic skip, such as "Skipped intro". */
internal val PlaybackMarkerKind.skippedLabel: StringResource
    get() = when (this) {
        PlaybackMarkerKind.INTRO, PlaybackMarkerKind.MIXED_INTRO -> MR.strings.anime_skipped_intro
        PlaybackMarkerKind.RECAP -> MR.strings.anime_skipped_recap
        PlaybackMarkerKind.OUTRO, PlaybackMarkerKind.MIXED_OUTRO -> MR.strings.anime_skipped_credits
        PlaybackMarkerKind.PREVIEW -> MR.strings.anime_skipped_preview
        PlaybackMarkerKind.CHAPTER -> error("A chapter is never skipped")
    }
