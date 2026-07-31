package mihon.entry.interactions.state

import mihon.entry.interactions.media.EntryPlaybackPreferencesSnapshot
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

interface EntryConsumptionInteraction {
    suspend fun setConsumed(
        entry: Entry,
        chapters: List<EntryChapter>,
        consumed: Boolean,
    ): List<EntryChapter>
}

interface EntryBookmarkInteraction {
    suspend fun setBookmarked(entry: Entry, chapters: List<EntryChapter>, bookmarked: Boolean)
}

interface EntryProgressInteraction {
    suspend fun snapshot(entry: Entry): EntryProgressSnapshot

    suspend fun restore(entry: Entry, snapshot: EntryProgressSnapshot)

    suspend fun copy(
        sourceEntry: Entry,
        targetEntry: Entry,
        resourceMappings: List<EntryProgressResourceMapping>,
    )

    suspend fun prepareMigration(
        sourceEntry: Entry,
        targetEntry: Entry,
        resourceMappings: List<EntryProgressResourceMapping>,
    ): EntryProgressSnapshot
}

interface EntryPlaybackPreferencesInteraction {
    suspend fun snapshot(entry: Entry): EntryPlaybackPreferencesSnapshot?
    suspend fun restore(entry: Entry, snapshot: EntryPlaybackPreferencesSnapshot)
    suspend fun copy(sourceEntry: Entry, targetEntry: Entry): Boolean
}
