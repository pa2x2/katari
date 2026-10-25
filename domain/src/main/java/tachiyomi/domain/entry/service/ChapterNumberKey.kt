package tachiyomi.domain.entry.service

import tachiyomi.domain.entry.model.EntryChapter

/**
 * What makes chapters the same chapter by number, as one chapter from two scanlators is. The number only counts within
 * its group, since numbering may restart in each volume: by number alone, Volume 2's first chapter would pass for
 * Volume 1's.
 */
data class ChapterNumberKey(
    val groupKey: String?,
    val number: Double,
)

val EntryChapter.numberKey: ChapterNumberKey
    get() = ChapterNumberKey(groupKey, chapterNumber)
