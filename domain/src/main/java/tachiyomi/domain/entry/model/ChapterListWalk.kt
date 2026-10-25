package tachiyomi.domain.entry.model

/**
 * An entry's chapter list being read part by part, newest first: where the next older part comes from and where its
 * chapters go.
 *
 * @property token the source's handle for the next older part.
 * @property nextSourceOrder [EntryChapter.sourceOrder] the next part's first chapter gets.
 * @property loaded how many of [total] units, such as the site's pages, the source has read; null when it doesn't say.
 */
data class ChapterListWalk(
    val entryId: Long,
    val token: String,
    val nextSourceOrder: Long,
    val loaded: Int?,
    val total: Int?,
)
