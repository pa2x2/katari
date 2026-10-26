package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.tachiyomi.source.entry.PlaybackMarkerKind
import eu.kanade.tachiyomi.ui.video.player.markers.VideoPlayerChapter
import eu.kanade.tachiyomi.ui.video.player.markers.VideoPlayerChapterName
import eu.kanade.tachiyomi.ui.video.player.playback.formatPlaybackTimestamp
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.KeyboardArrowDown
import mihon.presentation.kit.content.InlineIcon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.list.rowGroupItems
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The chapter playing, after the time under the timeline; tapping it lists the chapters. */
@Composable
internal fun VideoPlayerChapterLabel(
    chapter: VideoPlayerChapter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clickable(
                onClickLabel = stringResource(MR.strings.anime_chapters_show),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = KatariTheme.spacing.s, vertical = KatariTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs),
    ) {
        Text(
            text = "· ${chapter.name.text()}",
            modifier = Modifier.weight(1f, fill = false),
            role = TextRole.Body,
            tone = ContentTone.OnMedia,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
        InlineIcon(
            imageVector = MaterialSymbols.Rounded.KeyboardArrowDown,
            contentDescription = null,
            role = TextRole.Body,
            tone = ContentTone.OnMedia,
        )
    }
}

/** The settings' Chapters section: each chapter with where it starts, the playing one filled. */
internal fun LazyListScope.videoPlayerChapterItems(
    chapters: List<VideoPlayerChapter>,
    current: VideoPlayerChapter?,
    onSelect: (VideoPlayerChapter) -> Unit,
) {
    item(key = CHAPTERS_ITEM) {
        RowGroupHeader(stringResource(MR.strings.anime_chapters))
    }
    rowGroupItems(items = chapters, key = ::chapterItemKey, selected = { it == current }) { chapter ->
        SettingsRow(
            title = chapter.name.text(),
            trailing = {
                Text(
                    text = formatPlaybackTimestamp(chapter.startMs),
                    role = TextRole.Supporting,
                    tone = ContentTone.Secondary,
                )
            },
            onClick = { onSelect(chapter) },
        )
    }
}

/** The keys of the items [videoPlayerChapterItems] adds, in order. */
internal fun videoPlayerChapterItemKeys(chapters: List<VideoPlayerChapter>): List<String> =
    listOf(CHAPTERS_ITEM) + chapters.map(::chapterItemKey)

@Composable
private fun VideoPlayerChapterName.text(): String = when (this) {
    is VideoPlayerChapterName.Given -> text
    is VideoPlayerChapterName.Numbered -> stringResource(MR.strings.anime_chapter_numbered, number)
    is VideoPlayerChapterName.Marker -> stringResource(
        when (kind) {
            PlaybackMarkerKind.INTRO, PlaybackMarkerKind.MIXED_INTRO -> MR.strings.anime_chapter_intro
            PlaybackMarkerKind.RECAP -> MR.strings.anime_chapter_recap
            PlaybackMarkerKind.OUTRO, PlaybackMarkerKind.MIXED_OUTRO -> MR.strings.anime_chapter_credits
            PlaybackMarkerKind.PREVIEW -> MR.strings.anime_chapter_preview
            PlaybackMarkerKind.CHAPTER -> error("A chapter the source doesn't name is numbered")
        },
    )
}

private fun chapterItemKey(chapter: VideoPlayerChapter): String = "chapter_${chapter.startMs}"

internal const val CHAPTERS_ITEM = "chapters"
