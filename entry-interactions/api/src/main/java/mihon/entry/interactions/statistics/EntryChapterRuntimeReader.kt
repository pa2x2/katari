package mihon.entry.interactions.statistics

import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.statistics.entry.EntryChapterRuntime

/** Reads a chapter's runtime from the saved progress of a type whose progress records time, such as video. */
fun interface EntryChapterRuntimeReader {
    /** Null when [state] doesn't say how long its chapter runs. */
    fun read(state: EntryProgressState): EntryChapterRuntime?
}
