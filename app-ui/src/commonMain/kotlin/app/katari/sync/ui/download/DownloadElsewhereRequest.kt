package app.katari.sync.ui.download

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import app.katari.sync.client.downloadrequest.ChapterToRequest
import eu.kanade.presentation.entry.EntryTypePresentation
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Chapters another device of the account could be asked to download. [subject] says what is sent, under the picker's
 * title, such as a chapter's name and its entry's title; [sourceId] tells whether a device has the chapters' extension,
 * null when they come from several sources.
 */
@Immutable
sealed interface DownloadElsewhereRequest {
    val entryIds: Set<Long>
    val sourceId: Long?
    val subject: String

    /** Chapters already chosen, such as a row's or a selection's. */
    class Chapters(
        val chapters: List<ChapterToRequest>,
        override val sourceId: Long?,
        override val subject: String,
    ) : DownloadElsewhereRequest {
        override val entryIds: Set<Long> = chapters.mapTo(mutableSetOf()) { it.entryId }
    }

    /** An amount of an entry's chapters to pick once the device is picked, as the entry's Download menu offers. */
    class Amounts(
        override val entryIds: Set<Long>,
        override val sourceId: Long?,
        override val subject: String,
        val amounts: List<DownloadElsewhereAmount>,
    ) : DownloadElsewhereRequest
}

/** One of an entry's Download amounts, such as the next 5 chapters; this device works out which chapters it means. */
@Immutable
class DownloadElsewhereAmount(val label: String, val chapters: suspend () -> List<ChapterToRequest>)

/**
 * A request for [chapters], headed by the chapter's name for one and by how many there are for several, then by
 * [entryTitle] when they are all of one entry.
 */
@Composable
fun chaptersElsewhere(
    chapters: List<ChapterToRequest>,
    sourceId: Long?,
    entryTitle: String?,
    presentation: EntryTypePresentation,
): DownloadElsewhereRequest.Chapters {
    val what = chapters.singleOrNull()?.name
        ?: pluralStringResource(presentation.childCountPlural, chapters.size, chapters.size)
    val subject = entryTitle?.let { stringResource(MR.strings.sync_download_elsewhere_what, what, it) } ?: what
    return DownloadElsewhereRequest.Chapters(chapters, sourceId, subject)
}
