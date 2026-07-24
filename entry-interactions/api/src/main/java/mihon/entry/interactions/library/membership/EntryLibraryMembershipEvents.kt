package mihon.entry.interactions

import tachiyomi.domain.entry.model.Entry

data class EntryLibraryAddedEvent(
    val entry: Entry,
)

data class EntryLibraryRemovingEvent(
    val entries: List<Entry>,
)

data class EntryLibraryRemovedEvent(
    val entries: List<Entry>,
    val outcomes: EntryLibraryRemovalOutcomeSink,
)

fun interface EntryLibraryRemovalOutcomeSink {
    fun requireDownloadDecision(entry: Entry)
}
