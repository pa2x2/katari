package mihon.entry.interactions

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** Feature-owned boundary for contextual download options and selected-option execution. */
interface EntryDownloadOptionsFeature {
    fun isApplicable(type: EntryType): Boolean

    suspend fun resolve(
        context: Context,
        entry: Entry,
        chapter: EntryChapter,
    ): EntryDownloadOptionsResolution

    suspend fun download(
        entry: Entry,
        chapters: List<EntryChapter>,
        selection: EntryDownloadOptionSelection,
        startNow: Boolean = false,
    ): EntryDownloadOptionsActionResult
}

sealed interface EntryDownloadOptionsResolution {
    data class Resolved(
        val options: EntryDownloadOptions,
    ) : EntryDownloadOptionsResolution

    /** The provider applies to this type, but the current source/media context produced no options. */
    data object ContextuallyUnavailable : EntryDownloadOptionsResolution

    data object Inapplicable : EntryDownloadOptionsResolution
}

sealed interface EntryDownloadOptionsActionResult {
    data object Performed : EntryDownloadOptionsActionResult

    data object Inapplicable : EntryDownloadOptionsActionResult
}
