package eu.kanade.presentation.entry.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.tachiyomi.data.coil.SourceImage
import eu.kanade.tachiyomi.source.entry.ChapterKind
import eu.kanade.tachiyomi.ui.entry.chapters.EntryChapterList
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.presentation.core.i18n.stringResource

/** The title a chapter shows under the entry's display mode: its name, or its number. */
@Composable
fun entryChapterTitle(entry: Entry, chapter: EntryChapter): String {
    return if (entry.displayMode == Entry.CHAPTER_DISPLAY_NUMBER) {
        stringResource(
            entry.type.entryTypePresentation().childNumberDisplayLabel,
            formatChapterNumber(chapter.chapterNumber),
        )
    } else {
        chapter.name
    }
}

/** When the chapter came out: the source's own wording when it gave no date, since the stored date is then a guess. */
@Composable
fun entryChapterDate(chapter: EntryChapter): String = chapter.dateText ?: relativeDateText(chapter.dateUpload)

/** The tag naming a chapter outside the main story, such as "Filler"; null for a regular one. */
@Composable
fun entryChapterKindLabel(item: EntryChapterList.Item): String? {
    return when (item.chapter.kind) {
        ChapterKind.REGULAR -> null
        ChapterKind.FILLER -> stringResource(item.entry.type.entryTypePresentation().fillerChildLabel)
    }
}

/** Draws the chapter's still from its source, which may only serve it with its own headers; null without one. */
fun entryChapterPreview(item: EntryChapterList.Item): (@Composable (Modifier) -> Unit)? {
    val url = item.chapter.previewUrl ?: return null
    val sourceId = item.entry.source
    return { modifier ->
        AsyncImage(
            model = SourceImage(sourceId, url),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    }
}
