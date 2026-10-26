package tachiyomi.domain.entry.model

/**
 * An entry's chapter list being read part by part, newest first: where the next older part comes from and where its
 * chapters go.
 *
 * @property token the source's handle for the next older part.
 * @property nextSourceOrder [EntryChapter.sourceOrder] the next part's first chapter gets.
 * @property loaded how many of the site's [total] pages of the list the source has read; null when it doesn't say.
 * @property failure why the last attempt at the next part failed; null while the walk goes on.
 */
data class ChapterListWalk(
    val entryId: Long,
    val profileId: Long,
    val token: String,
    val nextSourceOrder: Long,
    val loaded: Int?,
    val total: Int?,
    val failure: String? = null,
)
