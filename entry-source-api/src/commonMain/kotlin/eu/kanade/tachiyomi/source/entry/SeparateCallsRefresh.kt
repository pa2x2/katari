package eu.kanade.tachiyomi.source.entry

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/*
 * UnifiedSource.refresh and the separate getContentDetails and getChapterList it replaced each default to the other: a
 * source built against SDK 2.6 or earlier implements only the separate calls and is refreshed through them, and a
 * source that implements only refresh still answers the separate calls. One that implements neither would bounce
 * between the defaults forever, so the separate-call defaults fail when they are reached from the refresh default of
 * the same source.
 */

/** Marks the coroutine of [source]'s default refresh while it makes the separate calls. */
private class SeparateCallsRefresh(val source: UnifiedSource) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<SeparateCallsRefresh>
}

@Suppress("DEPRECATION")
internal suspend fun UnifiedSource.refreshThroughSeparateCalls(
    entry: SEntry,
    existingChapters: List<SEntryChapter>,
    fetchDetails: Boolean,
    fetchChapters: Boolean,
): EntryRefresh = withContext(SeparateCallsRefresh(this)) {
    val details = if (fetchDetails) getContentDetails(entry) else entry
    // Hosts used to ask for the chapters with the stored entry updated from the details, so a memo the details set
    // reaches the chapter list. The url stays the host's: hosts never take it from the details.
    val chapterEntry = if (fetchDetails) details.copy().also { it.url = entry.url } else entry
    val source = this@refreshThroughSeparateCalls
    val chapters = when {
        !fetchChapters -> existingChapters
        source is IncrementalChapterSource -> source.getChapterList(chapterEntry, existingChapters)
        else -> getChapterList(chapterEntry)
    }
    EntryRefresh(details, chapters)
}

internal suspend fun UnifiedSource.refreshForSeparateCall(entry: SEntry, fetchDetails: Boolean): EntryRefresh {
    check(currentCoroutineContext()[SeparateCallsRefresh]?.source !== this) {
        "${this::class.qualifiedName} implements neither refresh nor getContentDetails and getChapterList"
    }
    return refresh(
        entry = entry,
        existingChapters = emptyList(),
        fetchDetails = fetchDetails,
        fetchChapters = !fetchDetails,
    )
}
