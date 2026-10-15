package mihon.feature.migration.list.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.presentation.entry.components.EntryCover
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.presentation.util.rememberResourceBitmapPainter
import eu.kanade.tachiyomi.R
import mihon.feature.migration.list.models.MigratingEntry
import mihon.feature.migration.list.models.MigrationMergeContext
import mihon.presentation.kit.content.Badge
import mihon.presentation.kit.content.BadgeGroup
import mihon.presentation.kit.content.BrowseMetrics
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.surface.MediaScrim
import mihon.presentation.kit.surface.MediaScrimEdge
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** One side of a migration row: an entry's cover with its title and chapter count, then its source and progress. */
@Composable
fun MigrationListItem(
    modifier: Modifier,
    entry: Entry,
    source: String,
    chapterCount: Int,
    latestChapter: Double?,
    mergeContext: MigrationMergeContext?,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .widthIn(max = BrowseMetrics.migrationCoverMaxWidth)
            .fillMaxWidth()
            .clip(KatariTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(KatariTheme.spacing.xs),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(EntryCover.Book.ratio),
        ) {
            EntryCover.Book(
                modifier = Modifier.fillMaxWidth(),
                data = entry,
            )
            MediaScrim(
                edge = MediaScrimEdge.Bottom,
                modifier = Modifier
                    .clip(KatariTheme.shapes.extraSmall)
                    .fillMaxHeight(TITLE_SCRIM_FRACTION)
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
            ) {}
            Text(
                text = entry.title,
                modifier = Modifier
                    .padding(KatariTheme.spacing.s)
                    .align(Alignment.BottomStart),
                role = TextRole.Caption,
                tone = ContentTone.OnMedia,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
            )
            BadgeGroup(modifier = Modifier.padding(KatariTheme.spacing.xs)) {
                Badge(text = "$chapterCount")
            }
        }

        Column(
            modifier = Modifier.padding(KatariTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs),
        ) {
            Text(
                text = source,
                role = TextRole.Label,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
            mergeContext?.let {
                Text(
                    text = migrationMergeContextLabel(it),
                    role = TextRole.Badge,
                    tone = ContentTone.Primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val formattedLatestChapters = remember(latestChapter) {
                latestChapter?.let(::formatChapterNumber)
            }
            Text(
                text = stringResource(
                    MR.strings.migrationListScreen_latestChapterLabel,
                    formattedLatestChapters ?: stringResource(MR.strings.migrationListScreen_unknownLatestChapter),
                ),
                role = TextRole.Body,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }
}

/** The match side of a migration row: a wait indicator while searching, the match, or a note that there is none. */
@Composable
fun MigrationListItemResult(
    modifier: Modifier,
    result: MigratingEntry.SearchResult,
    onItemClick: (Entry, MigrationMergeContext?) -> Unit,
) {
    Box(modifier.height(IntrinsicSize.Min)) {
        when (result) {
            MigratingEntry.SearchResult.Searching -> {
                Box(
                    modifier = Modifier
                        .widthIn(max = BrowseMetrics.migrationCoverMaxWidth)
                        .fillMaxSize()
                        .aspectRatio(EntryCover.Book.ratio),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingIndicator()
                }
            }
            MigratingEntry.SearchResult.NotFound -> {
                Column(
                    Modifier
                        .widthIn(max = BrowseMetrics.migrationCoverMaxWidth)
                        .fillMaxSize()
                        .padding(KatariTheme.spacing.xs),
                ) {
                    Image(
                        painter = rememberResourceBitmapPainter(id = R.drawable.cover_error),
                        contentDescription = stringResource(MR.strings.label_more),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(EntryCover.Book.ratio)
                            .clip(KatariTheme.shapes.extraSmall),
                        contentScale = ContentScale.Crop,
                    )
                    Text(
                        text = stringResource(MR.strings.migrationListScreen_noMatchFoundText),
                        modifier = Modifier.padding(KatariTheme.spacing.xs),
                        role = TextRole.Label,
                    )
                }
            }
            is MigratingEntry.SearchResult.Success -> {
                MigrationListItem(
                    modifier = Modifier.fillMaxSize(),
                    entry = result.entry,
                    source = result.source,
                    chapterCount = result.chapterCount,
                    latestChapter = result.latestChapter,
                    mergeContext = result.mergeContext,
                    onClick = { onItemClick(result.entry, result.mergeContext) },
                )
            }
        }
    }
}

@Composable
private fun migrationMergeContextLabel(context: MigrationMergeContext): String {
    return if (context.isRoot) {
        stringResource(MR.strings.migrationListScreen_mergeRootLabel)
    } else {
        stringResource(MR.strings.migrationListScreen_mergeMemberLabel, context.rootTitle)
    }
}

private const val TITLE_SCRIM_FRACTION = 0.4f
