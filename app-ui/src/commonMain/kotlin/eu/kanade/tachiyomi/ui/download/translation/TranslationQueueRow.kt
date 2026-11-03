package eu.kanade.tachiyomi.ui.download.translation

import androidx.compose.runtime.Immutable
import mihon.entry.interactions.translate.EntryTranslateStatus
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** A chapter in the translation queue with the series it belongs to. */
@Immutable
data class TranslationQueueRow(
    val entry: Entry,
    val chapter: EntryChapter,
    val status: EntryTranslateStatus,
)
