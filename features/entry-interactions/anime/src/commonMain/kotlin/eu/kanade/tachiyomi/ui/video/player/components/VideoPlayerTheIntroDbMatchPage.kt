package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import app.katari.entry.interactions.anime.introtimes.IntroTimesService
import app.katari.entry.interactions.anime.introtimes.theintrodb.TheIntroDbCandidate
import app.katari.presentation.kit.content.CoverBacking
import app.katari.presentation.kit.content.CoverFrame
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.content.coverFrame
import app.katari.presentation.kit.content.rememberArtworkPlaceholder
import app.katari.presentation.kit.layout.RowListScope
import app.katari.presentation.kit.list.CoverRow
import app.katari.presentation.kit.list.RowGroupAction
import app.katari.presentation.kit.theme.TextRole
import coil3.compose.AsyncImage
import eu.kanade.tachiyomi.ui.video.player.introtimes.VideoPlayerIntroTimesMatch
import tachiyomi.domain.entry.model.TheIntroDbMedia
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * TheIntroDB's match page: it searches Wikidata and TVmaze for series and films, and points at a series' episodes in
 * TVmaze's list of them, which it credits.
 */
@Composable
internal fun VideoPlayerTheIntroDbMatchPage(
    frame: VideoPlayerSettingsFrame,
    match: VideoPlayerIntroTimesMatch<TheIntroDbCandidate, TheIntroDbMedia>,
    onUse: (TheIntroDbMedia?) -> Unit,
    onBack: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    VideoPlayerMatchPage(
        frame = frame,
        service = IntroTimesService.THEINTRODB,
        match = match,
        content = VideoPlayerMatchPageContent(
            searchPlaceholder = stringResource(MR.strings.anime_intro_times_media_search_placeholder),
            searchFailed = stringResource(MR.strings.anime_intro_times_media_search_failed),
            openFailed = stringResource(MR.strings.anime_intro_times_episodes_failed),
            inUse = TheIntroDbCandidate::isSameAs,
            candidateRow = { candidate, onClick -> CandidateRow(candidate, onClick) },
            credit = { tvMazeCredit() },
        ),
        onUse = onUse,
        onBack = onBack,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    )
}

// TVmaze shares its data under CC BY-SA, which asks for a credit with a link wherever it shows.
private fun RowListScope.tvMazeCredit() {
    item(key = CREDIT_ITEM) {
        val uriHandler = LocalUriHandler.current
        RowGroupAction(
            label = stringResource(MR.strings.anime_intro_times_tvmaze_credit),
            onClick = { uriHandler.openUri(TVMAZE_URL) },
        )
    }
}

@Composable
private fun CandidateRow(candidate: TheIntroDbCandidate, onClick: () -> Unit) {
    CoverRow(
        cover = {
            AsyncImage(
                model = candidate.coverUrl,
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
        Text(candidate.title, role = TextRole.ItemTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
        val kind = stringResource(
            if (candidate.film) MR.strings.anime_intro_times_kind_film else MR.strings.anime_intro_times_kind_series,
        )
        Text(
            listOfNotNull(kind, candidate.year?.toString()).joinToString(" · "),
            role = TextRole.Supporting,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private const val TVMAZE_URL = "https://www.tvmaze.com"
private const val CREDIT_ITEM = "credit"
