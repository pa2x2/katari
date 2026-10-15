package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerEpisodeListEntry
import mihon.entry.interactions.anime.state.durationMs
import mihon.entry.interactions.anime.state.positionMs
import mihon.entry.viewer.chrome.ui.OverlayListDrawer
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredroundedfilled.ViewList
import mihon.icons.materialsymbols.rounded.KeyboardArrowDown
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import mihon.icons.materialsymbols.roundedfilled.PlayArrow
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LinearProgressIndicator
import mihon.presentation.kit.surface.HorizontalDivider
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun VideoPlayerEpisodesDrawer(
    visible: Boolean,
    anime: Entry,
    episodeListItems: List<VideoPlayerEpisodeListEntry>,
    expandedMemberIds: Set<Long>,
    currentEpisodeId: Long,
    playbackStateByEpisodeId: Map<Long, EntryProgressState>,
    sourceAvailable: Boolean,
    onEpisodeClick: (EntryChapter) -> Unit,
    onMemberClick: (Long) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val shownItems = remember(episodeListItems, expandedMemberIds) {
        episodeListItems.withoutCollapsedMembers(expandedMemberIds)
    }
    val currentEpisodeIndex = shownItems.indexOfFirst { entry ->
        (entry as? VideoPlayerEpisodeListEntry.Item)?.episode?.id == currentEpisodeId
    }

    LaunchedEffect(visible, currentEpisodeId, episodeListItems) {
        if (visible && currentEpisodeIndex >= 0) {
            listState.scrollToItem(currentEpisodeIndex)
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
        ) {
            videoPlayerEpisodeItems(
                anime = anime,
                episodeListItems = shownItems,
                expandedMemberIds = expandedMemberIds,
                currentEpisodeId = currentEpisodeId,
                playbackStateByEpisodeId = playbackStateByEpisodeId,
                sourceAvailable = sourceAvailable,
                onEpisodeClick = onEpisodeClick,
                onMemberClick = onMemberClick,
            )
        }
    }
}

private fun LazyListScope.videoPlayerEpisodeItems(
    anime: Entry,
    episodeListItems: List<VideoPlayerEpisodeListEntry>,
    expandedMemberIds: Set<Long>,
    currentEpisodeId: Long,
    playbackStateByEpisodeId: Map<Long, EntryProgressState>,
    sourceAvailable: Boolean,
    onEpisodeClick: (EntryChapter) -> Unit,
    onMemberClick: (Long) -> Unit,
) {
    if (episodeListItems.isEmpty()) {
        item {
            Text(
                text = stringResource(MR.strings.anime_no_episodes),
                modifier = Modifier.padding(
                    horizontal = KatariTheme.spacing.contentInset,
                    vertical = KatariTheme.spacing.s,
                ),
                role = TextRole.Body,
                tone = ContentTone.Secondary,
            )
        }
        return
    }
    // A merged list names each member in its header, so rows don't repeat the merged entry's title.
    val showsEntryTitle = episodeListItems.none { it is VideoPlayerEpisodeListEntry.MemberHeader }

    items(
        items = episodeListItems,
        key = {
            when (it) {
                is VideoPlayerEpisodeListEntry.Item -> "episode-${it.episode.id}"
                is VideoPlayerEpisodeListEntry.MemberHeader -> "member-${it.animeId}"
            }
        },
    ) { entry ->
        when (entry) {
            is VideoPlayerEpisodeListEntry.MemberHeader -> {
                VideoPlayerEpisodeMemberHeader(
                    header = entry,
                    expanded = entry.animeId in expandedMemberIds,
                    onClick = { onMemberClick(entry.animeId) },
                )
            }
            is VideoPlayerEpisodeListEntry.Item -> {
                val episode = entry.episode
                VideoPlayerEpisodeRow(
                    anime = anime,
                    episode = episode,
                    selected = episode.id == currentEpisodeId,
                    playbackState = playbackStateByEpisodeId[episode.id],
                    showsEntryTitle = showsEntryTitle,
                    enabled = true,
                    onClick = { onEpisodeClick(episode) },
                )
            }
        }
    }
}

@Composable
private fun VideoPlayerEpisodeMemberHeader(
    header: VideoPlayerEpisodeListEntry.MemberHeader,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val summary = listOfNotNull(
        header.sourceName,
        pluralStringResource(MR.plurals.anime_num_episodes, header.episodeCount, header.episodeCount),
        header.unwatchedCount.takeIf { it > 0 }?.let {
            stringResource(
                MR.strings.entry_member_unconsumed_count,
                stringResource(MR.strings.action_filter_unwatched),
                it,
            )
        },
    ).joinToString(" · ")
    val spacing = KatariTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = spacing.contentInset, top = spacing.s, end = spacing.m, bottom = spacing.s),
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = header.title,
                role = TextRole.GroupLabel,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = summary, role = TextRole.Supporting)
        }
        Icon(
            imageVector = if (expanded) {
                MaterialSymbols.Rounded.KeyboardArrowUp
            } else {
                MaterialSymbols.Rounded.KeyboardArrowDown
            },
            contentDescription = null,
            tone = ContentTone.Secondary,
        )
    }
}

/** Drops the episodes listed under collapsed member headers. */
private fun List<VideoPlayerEpisodeListEntry>.withoutCollapsedMembers(
    expandedMemberIds: Set<Long>,
): List<VideoPlayerEpisodeListEntry> {
    var inCollapsedMember = false
    return filter { entry ->
        when (entry) {
            is VideoPlayerEpisodeListEntry.MemberHeader -> {
                inCollapsedMember = entry.animeId !in expandedMemberIds
                true
            }
            is VideoPlayerEpisodeListEntry.Item -> !inCollapsedMember
        }
    }
}

@Composable
private fun VideoPlayerEpisodeRow(
    anime: Entry,
    episode: EntryChapter,
    selected: Boolean,
    playbackState: EntryProgressState?,
    showsEntryTitle: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val progress = playbackState.progressFraction()
    val completed = episode.read || playbackState?.completed == true
    val titleTone = if (completed && !selected) ContentTone.Secondary else ContentTone.Inherit
    val spacing = KatariTheme.spacing

    Column {
        Surface(
            onClick = onClick,
            container = if (selected) SurfaceContainer.Selected else SurfaceContainer.None,
            enabled = enabled,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.contentInset, vertical = spacing.m),
                horizontalArrangement = Arrangement.spacedBy(spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selected) {
                    Icon(imageVector = MaterialSymbols.RoundedFilled.PlayArrow, contentDescription = null)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    Text(
                        text = episode.name.ifBlank { episode.url },
                        role = TextRole.Body,
                        tone = titleTone,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (showsEntryTitle) {
                        Text(
                            text = anime.displayTitle,
                            role = TextRole.Supporting,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (progress != null) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = spacing.xxs),
                        )
                    }
                }
            }
        }
        HorizontalDivider()
    }
}

private fun EntryProgressState?.progressFraction(): Float? {
    if (this == null || durationMs <= 0L || positionMs <= 0L || completed) return null
    return (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
}
