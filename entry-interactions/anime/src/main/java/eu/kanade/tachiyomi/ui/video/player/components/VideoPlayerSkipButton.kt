package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.source.entry.PlaybackMarkerKind
import eu.kanade.tachiyomi.ui.video.player.markers.VideoPlayerSkipOffer
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.roundedfilled.SkipNext
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.OverlayPill
import mihon.presentation.kit.surface.OverlayStyle
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Skips the intro, recap, credits or preview playing, or moves on to the next episode when only those are left. */
@Composable
internal fun VideoPlayerSkipButton(
    offer: VideoPlayerSkipOffer,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = KatariTheme.spacing
    OverlayPill(modifier = modifier, style = OverlayStyle.Scrim, onClick = onClick) {
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
            Text(
                text = stringResource(
                    if (offer.goesToNextEpisode) {
                        MR.strings.anime_playback_next_episode
                    } else {
                        offer.marker.kind.skipLabel
                    },
                ),
                role = TextRole.Label,
                maxLines = 1,
            )
        }
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
