package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerEpisodeListEntry
import mihon.entry.interactions.anime.state.durationMs
import mihon.entry.interactions.anime.state.positionMs
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredroundedfilled.ViewList
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.list.ChapterRow
import mihon.presentation.kit.list.CollapsibleRowGroupHeader
import mihon.presentation.kit.list.CollapsibleRowGroupSubheader
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupNote
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.sheet.OverlayListDrawer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.domain.entry.model.ChapterGroupId
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun VideoPlayerEpisodesDrawer(
    visible: Boolean,
    episodeListItems: List<VideoPlayerEpisodeListEntry>,
    expandedMemberIds: Set<Long>,
    expandedGroupIds: Set<ChapterGroupId>,
    currentEpisodeId: Long,
    /** The episode opening over [currentEpisodeId], marked as such; until it plays or fails no episode can be picked. */
    openingEpisodeId: Long?,
    playbackStateByEpisodeId: Map<Long, EntryProgressState>,
    sourceAvailable: Boolean,
    onEpisodeClick: (EntryChapter) -> Unit,
    onMemberClick: (Long) -> Unit,
    onGroupClick: (ChapterGroupId) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val shownItems = remember(episodeListItems, expandedMemberIds, expandedGroupIds) {
        episodeListItems.withoutCollapsedSections(expandedMemberIds, expandedGroupIds)
    }
    val currentEpisodeIndex = shownItems.indexOfFirst { entry ->
        (entry as? VideoPlayerEpisodeListEntry.Item)?.episode?.id == currentEpisodeId
    }
    // When the playing episode opens its member or group, the headers right above it stay in view to name it
    val scrollIndex = (currentEpisodeIndex downTo 1)
        .firstOrNull { shownItems[it - 1] is VideoPlayerEpisodeListEntry.Item }
        ?: 0

    LaunchedEffect(visible, currentEpisodeId, episodeListItems) {
        if (visible && currentEpisodeIndex >= 0) {
            listState.scrollToItem(scrollIndex)
        }
    }

    OverlayListDrawer(
        visible = visible,
        title = stringResource(MR.strings.episodes),
        icon = MaterialSymbols.AutoMirroredRoundedFilled.ViewList,
        closeDescription = stringResource(MR.strings.action_close),
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        drawerModifier = Modifier.videoPlayerSafeContentPadding(includeTop = true, includeBottom = true),
    ) {
        RowList(
            modifier = Modifier.fillMaxSize(),
            fastScroll = true,
            state = listState,
            contentPadding = PaddingValues(bottom = KatariTheme.spacing.m),
        ) {
            videoPlayerEpisodeItems(
                episodeListItems = shownItems,
                expandedMemberIds = expandedMemberIds,
                expandedGroupIds = expandedGroupIds,
                currentEpisodeId = currentEpisodeId,
                openingEpisodeId = openingEpisodeId,
                playbackStateByEpisodeId = playbackStateByEpisodeId,
                sourceAvailable = sourceAvailable,
                onEpisodeClick = onEpisodeClick,
                onMemberClick = onMemberClick,
                onGroupClick = onGroupClick,
            )
        }
    }
}

private fun LazyListScope.videoPlayerEpisodeItems(
    episodeListItems: List<VideoPlayerEpisodeListEntry>,
    expandedMemberIds: Set<Long>,
    expandedGroupIds: Set<ChapterGroupId>,
    currentEpisodeId: Long,
    openingEpisodeId: Long?,
    playbackStateByEpisodeId: Map<Long, EntryProgressState>,
    sourceAvailable: Boolean,
    onEpisodeClick: (EntryChapter) -> Unit,
    onMemberClick: (Long) -> Unit,
    onGroupClick: (ChapterGroupId) -> Unit,
) {
    if (episodeListItems.isEmpty()) {
        item { RowGroupNote(stringResource(MR.strings.anime_no_episodes)) }
        return
    }
    // Runs of episodes between member headers, and group headers outside a merge, are each a card; a merged member's
    // group headers are rows of its card.
    val positions = RowGroupPosition.ofRuns(episodeListItems) {
        it is VideoPlayerEpisodeListEntry.Item || (it is VideoPlayerEpisodeListEntry.GroupHeader && it.nested)
    }
    itemsIndexed(
        items = episodeListItems,
        key = { _, entry ->
            when (entry) {
                is VideoPlayerEpisodeListEntry.Item -> "episode-${entry.episode.id}"
                is VideoPlayerEpisodeListEntry.MemberHeader -> "member-${entry.animeId}"
                is VideoPlayerEpisodeListEntry.GroupHeader ->
                    "group-${entry.group.entryId}-${entry.group.groupKey?.let { "key-$it" } ?: "none"}"
            }
        },
    ) { index, entry ->
        when (entry) {
            is VideoPlayerEpisodeListEntry.MemberHeader -> {
                CollapsibleRowGroupHeader(
                    title = entry.title,
                    summary = episodeSummary(entry.sourceName, entry.episodeCount, entry.unwatchedCount),
                    expanded = entry.animeId in expandedMemberIds,
                    onClick = { onMemberClick(entry.animeId) },
                )
            }
            is VideoPlayerEpisodeListEntry.GroupHeader -> {
                val title = entry.title ?: stringResource(MR.strings.chapter_group_other_episodes)
                val summary = if (entry.episodeCount == 0) {
                    stringResource(MR.strings.chapter_group_no_episodes_yet)
                } else {
                    episodeSummary(sourceName = null, entry.episodeCount, entry.unwatchedCount)
                }
                val expanded = entry.group in expandedGroupIds
                if (entry.nested) {
                    RowGroupItem(position = positions[index] ?: RowGroupPosition.Alone) {
                        CollapsibleRowGroupSubheader(title, summary, expanded, onClick = { onGroupClick(entry.group) })
                    }
                } else {
                    CollapsibleRowGroupHeader(title, summary, expanded, onClick = { onGroupClick(entry.group) })
                }
            }
            is VideoPlayerEpisodeListEntry.Item -> {
                val episode = entry.episode
                val selected = episode.id == currentEpisodeId
                RowGroupItem(position = positions[index] ?: RowGroupPosition.Alone, selected = selected) {
                    VideoPlayerEpisodeRow(
                        episode = episode,
                        playbackState = playbackStateByEpisodeId[episode.id],
                        opening = episode.id == openingEpisodeId,
                        enabled = openingEpisodeId == null,
                        onClick = { onEpisodeClick(episode) },
                    )
                }
            }
        }
    }
}

@Composable
private fun episodeSummary(sourceName: String?, episodeCount: Int, unwatchedCount: Int): String {
    return listOfNotNull(
        sourceName,
        pluralStringResource(MR.plurals.anime_num_episodes, episodeCount, episodeCount),
        unwatchedCount.takeIf { it > 0 }?.let {
            stringResource(
                MR.strings.entry_unconsumed_count,
                stringResource(MR.strings.action_filter_unwatched),
                it,
            )
        },
    ).joinToString(" · ")
}

/** Drops the episodes listed under collapsed member headers, and under collapsed group headers within a member. */
private fun List<VideoPlayerEpisodeListEntry>.withoutCollapsedSections(
    expandedMemberIds: Set<Long>,
    expandedGroupIds: Set<ChapterGroupId>,
): List<VideoPlayerEpisodeListEntry> {
    var inCollapsedMember = false
    var inCollapsedGroup = false
    return filter { entry ->
        when (entry) {
            is VideoPlayerEpisodeListEntry.MemberHeader -> {
                inCollapsedMember = entry.animeId !in expandedMemberIds
                inCollapsedGroup = false
                true
            }
            is VideoPlayerEpisodeListEntry.GroupHeader -> {
                inCollapsedGroup = entry.group !in expandedGroupIds
                !inCollapsedMember
            }
            is VideoPlayerEpisodeListEntry.Item -> !inCollapsedMember && !inCollapsedGroup
        }
    }
}

/** The row slice's fill marks the episode playing, so the row itself doesn't fill again. */
@Composable
private fun VideoPlayerEpisodeRow(
    episode: EntryChapter,
    playbackState: EntryProgressState?,
    opening: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val progress = playbackState.episodeProgress()
    ChapterRow(
        title = episode.name.ifBlank { episode.url },
        read = episode.read || playbackState?.completed == true,
        bookmark = episode.bookmark,
        onClick = onClick,
        progress = progress?.let {
            pluralStringResource(MR.plurals.anime_episode_minutes_left, it.minutesLeft, it.minutesLeft)
        },
        progressFraction = progress?.fraction,
        enabled = enabled,
        trailing = if (opening) {
            {
                LoadingIndicator(modifier = Modifier.size(KatariTheme.sizes.iconSmall), tone = ContentTone.Primary)
                Text(
                    text = stringResource(MR.strings.anime_playback_episode_opening),
                    role = TextRole.Supporting,
                    tone = ContentTone.Primary,
                )
            }
        } else {
            null
        },
    )
}

private class EpisodeProgress(val fraction: Float, val minutesLeft: Int)

private fun EntryProgressState?.episodeProgress(): EpisodeProgress? {
    if (this == null || durationMs <= 0L || positionMs <= 0L || completed) return null
    val remainingMs = (durationMs - positionMs).coerceAtLeast(0L)
    return EpisodeProgress(
        fraction = (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f),
        // Rounded up, so the last seconds of an episode still read "1 min left" rather than 0.
        minutesLeft = ((remainingMs + MS_PER_MINUTE - 1) / MS_PER_MINUTE).toInt().coerceAtLeast(1),
    )
}

private const val MS_PER_MINUTE = 60_000L
