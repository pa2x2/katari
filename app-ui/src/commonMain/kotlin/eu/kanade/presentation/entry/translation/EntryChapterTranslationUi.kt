package eu.kanade.presentation.entry.translation

import androidx.compose.runtime.Immutable
import app.katari.entry.interactions.translate.EntryTranslateStatus
import eu.kanade.tachiyomi.ui.entry.chapters.EntryChapterList

/** How the entry screen shows and changes its chapters' translations. */
@Immutable
class EntryChapterTranslationUi(
    val statuses: Map<Long, EntryTranslateStatus>,
    val onAction: (List<EntryChapterList.Item>, ChapterTranslateAction) -> Unit,
)
