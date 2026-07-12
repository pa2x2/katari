package tachiyomi.domain.entry.service

import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.library.service.entrySortComparator

fun getChapterSort(
    entry: Entry,
    sortDescending: Boolean = entry.sortDescending(),
): (
    EntryChapter,
    EntryChapter,
) -> Int {
    val comparator = entrySortComparator(
        sorting = entry.sorting,
        sortDescending = sortDescending,
        sortingSourceFlag = Entry.CHAPTER_SORTING_SOURCE,
        sortingNumberFlag = Entry.CHAPTER_SORTING_NUMBER,
        sortingUploadDateFlag = Entry.CHAPTER_SORTING_UPLOAD_DATE,
        sortingAlphabetFlag = Entry.CHAPTER_SORTING_ALPHABET,
        numberSelector = EntryChapter::chapterNumber,
        dateUploadSelector = EntryChapter::dateUpload,
        nameSelector = EntryChapter::name,
        urlSelector = EntryChapter::url,
        sourceOrderSelector = EntryChapter::sourceOrder,
    )
    return { c1, c2 -> comparator.compare(c1, c2) }
}
