package eu.kanade.presentation.entry.translation

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.ui.entry.EntryChapterList
import mihon.entry.interactions.translate.EntryTranslateStatus

/** How the entry screen shows and changes its chapters' translations. */
@Immutable
class EntryChapterTranslationUi(
    val statuses: Map<Long, EntryTranslateStatus>,
    val onAction: (List<EntryChapterList.Item>, ChapterTranslateAction) -> Unit,
)
