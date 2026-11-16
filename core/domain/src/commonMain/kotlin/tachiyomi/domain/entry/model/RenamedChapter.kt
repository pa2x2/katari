package tachiyomi.domain.entry.model

/**
 * A change to a stored chapter's name, scanlator or URL, holding what they were before it. The chapter's download is
 * still stored under a name made from them until it is moved.
 */
data class RenamedChapter(
    val id: Long,
    val chapterId: Long,
    val entryId: Long,
    val profileId: Long,
    val formerName: String,
    val formerScanlator: String?,
    val formerUrl: String,
)
