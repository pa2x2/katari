package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import app.katari.entry.interactions.anime.introtimes.IntroTimesService
import app.katari.entry.interactions.anime.introtimes.aniskip.AniListSeries
import app.katari.presentation.kit.content.CoverBacking
import app.katari.presentation.kit.content.CoverFrame
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.content.coverFrame
import app.katari.presentation.kit.content.rememberArtworkPlaceholder
import app.katari.presentation.kit.list.CoverRow
import app.katari.presentation.kit.theme.TextRole
import coil3.compose.AsyncImage
import eu.kanade.tachiyomi.ui.video.player.introtimes.VideoPlayerIntroTimesMatch
import tachiyomi.domain.entry.model.IntroTimesSeries
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/** AniSkip's match page: it searches AniList, whose results keep a show's seasons together in the order they aired. */
@Composable
internal fun VideoPlayerAniSkipMatchPage(
    frame: VideoPlayerSettingsFrame,
    match: VideoPlayerIntroTimesMatch<AniListSeries, IntroTimesSeries>,
    onUse: (IntroTimesSeries?) -> Unit,
    onBack: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unreachable = stringResource(MR.strings.anime_intro_times_search_failed)
    VideoPlayerMatchPage(
        frame = frame,
        service = IntroTimesService.ANISKIP,
        match = match,
        content = VideoPlayerMatchPageContent(
            searchPlaceholder = stringResource(MR.strings.anime_intro_times_search_placeholder),
            searchFailed = unreachable,
            openFailed = unreachable,
            inUse = { candidate, series -> candidate.anilistId == series.anilistId },
            candidateRow = { series, onClick -> SeriesRow(series, onClick) },
        ),
        onUse = onUse,
        onBack = onBack,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    )
}

@Composable
private fun SeriesRow(series: AniListSeries, onClick: () -> Unit) {
    CoverRow(
        cover = {
            AsyncImage(
                model = series.coverUrl,
                contentDescription = null,
                placeholder = rememberArtworkPlaceholder(),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .coverFrame(CoverFrame.Thumbnail, CoverBacking.Placeholder),
            )
        },
        onClick = onClick,
    ) {
        Text(series.title, role = TextRole.ItemTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
        val details = listOfNotNull(
            series.format?.let { formatLabel(it) },
            series.year?.toString(),
            series.episodes?.let { pluralStringResource(MR.plurals.anime_num_episodes, it, it) },
        )
        if (details.isNotEmpty()) {
            Text(
                details.joinToString(" · "),
                role = TextRole.Supporting,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// AniList's MediaFormat values for anime.
@Composable
private fun formatLabel(format: String): String? = when (format) {
    "TV" -> stringResource(MR.strings.anime_series_format_tv)
    "TV_SHORT" -> stringResource(MR.strings.anime_series_format_tv_short)
    "MOVIE" -> stringResource(MR.strings.anime_series_format_movie)
    "SPECIAL" -> stringResource(MR.strings.anime_series_format_special)
    "OVA" -> stringResource(MR.strings.anime_series_format_ova)
    "ONA" -> stringResource(MR.strings.anime_series_format_ona)
    "MUSIC" -> stringResource(MR.strings.anime_series_format_music)
    else -> null
}
