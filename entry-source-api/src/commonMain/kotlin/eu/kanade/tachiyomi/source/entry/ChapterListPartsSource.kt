package eu.kanade.tachiyomi.source.entry

/**
 * A source whose chapter lists may arrive in parts: [refresh] returns the newest part with [ChapterListRest.More], and
 * the host then reads each older part in the background, one at a time.
 */
interface ChapterListPartsSource : UnifiedSource {

    /**
     * Returns the part of [entry]'s chapters that [token] names, the next older one after the part whose
     * [ChapterListRest.More] gave it. Throw to stop the walk; the host retries later with the same token.
     */
    suspend fun getOlderChapters(entry: SEntry, token: String): ChapterListPart
}

/**
 * One older part of a chapter list.
 *
 * @property chapters newest first, like those [UnifiedSource.refresh] returns. A chapter an earlier part of the same walk
 * listed is ignored.
 * @property chapterGroups the groups [chapters] refer to, in reading order. A group may span parts; declare it in each.
 * @property rest what follows this part.
 */
class ChapterListPart(
    val chapters: List<SEntryChapter>,
    val rest: ChapterListRest,
    val chapterGroups: List<SEntryChapterGroup> = emptyList(),
)
