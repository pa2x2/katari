package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerEpisodeListEntry
import mihon.entry.interactions.anime.durationMs
import mihon.entry.interactions.anime.positionMs
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.OverlayActionButton
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun VideoPlayerEpisodesDrawer(
    visible: Boolean,
    anime: Entry,
    episodeListItems: List<VideoPlayerEpisodeListEntry>,
    currentEpisodeId: Long,
    playbackStateByEpisodeId: Map<Long, EntryProgressState>,
    sourceAvailable: Boolean,
    onEpisodeClick: (EntryChapter) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val currentEpisodeIndex = episodeListItems.indexOfFirst { entry ->
        (entry as? VideoPlayerEpisodeListEntry.Item)?.episode?.id == currentEpisodeId
    }

    LaunchedEffect(visible, currentEpisodeId, episodeListItems) {
        if (visible && currentEpisodeIndex >= 0) {
            listState.scrollToItem(currentEpisodeIndex)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.matchParentSize(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.38f))
                    .clickable(onClick = onDismissRequest),
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(0.94f)
                .videoPlayerSafeContentPadding(includeTop = true, includeBottom = true)
                .padding(end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
                exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(min = 280.dp, max = 340.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 14.dp, end = 8.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ViewList,
                                    contentDescription = null,
                                )
                                Text(
                                    text = stringResource(MR.strings.episodes),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            IconButton(onClick = onDismissRequest) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(MR.strings.action_close),
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                        ) {
                            videoPlayerEpisodeItems(
                                anime = anime,
                                episodeListItems = episodeListItems,
                                currentEpisodeId = currentEpisodeId,
                                playbackStateByEpisodeId = playbackStateByEpisodeId,
                                sourceAvailable = sourceAvailable,
                                onEpisodeClick = onEpisodeClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.videoPlayerEpisodeItems(
    anime: Entry,
    episodeListItems: List<VideoPlayerEpisodeListEntry>,
    currentEpisodeId: Long,
    playbackStateByEpisodeId: Map<Long, EntryProgressState>,
    sourceAvailable: Boolean,
    onEpisodeClick: (EntryChapter) -> Unit,
) {
    if (episodeListItems.isEmpty()) {
        item {
            Text(
                text = stringResource(MR.strings.anime_no_episodes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        return
    }

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
                VideoPlayerEpisodeMemberHeader(title = entry.title)
            }
            is VideoPlayerEpisodeListEntry.Item -> {
                val episode = entry.episode
                VideoPlayerEpisodeRow(
                    anime = anime,
                    episode = episode,
                    selected = episode.id == currentEpisodeId,
                    playbackState = playbackStateByEpisodeId[episode.id],
                    enabled = true,
                    onClick = { onEpisodeClick(episode) },
                )
            }
        }
    }
}

@Composable
private fun VideoPlayerEpisodeMemberHeader(
    title: String,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun VideoPlayerEpisodeRow(
    anime: Entry,
    episode: EntryChapter,
    selected: Boolean,
    playbackState: EntryProgressState?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val progress = playbackState.progressFraction()
    val completed = episode.read || playbackState?.completed == true
    val contentColor = when {
        selected -> MaterialTheme.colorScheme.onSecondaryContainer
        completed -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                    } else {
                        Color.Transparent
                    },
                )
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = contentColor,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = episode.name.ifBlank { episode.url },
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = anime.displayTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (progress != null) {
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .fillMaxWidth(progress)
                            .height(3.dp)
                            .background(MaterialTheme.colorScheme.primary),
                    )
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

@Composable
internal fun VideoPlayerTimelineToolbar(
    onOpenEpisodes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 0.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        OverlayActionButton(
            title = stringResource(MR.strings.episodes),
            icon = Icons.AutoMirrored.Filled.ViewList,
            onClick = onOpenEpisodes,
            contentDescription = stringResource(MR.strings.action_view_episodes),
            containerColor = Color.Transparent,
            contentColor = Color.White.copy(alpha = 0.88f),
        )
    }
}
