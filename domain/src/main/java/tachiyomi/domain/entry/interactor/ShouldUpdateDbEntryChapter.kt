package tachiyomi.domain.entry.interactor

import tachiyomi.domain.entry.model.EntryChapter

class ShouldUpdateDbEntryChapter {

    fun await(dbChapter: EntryChapter, sourceChapter: EntryChapter): Boolean {
        return dbChapter.scanlator != sourceChapter.scanlator ||
            dbChapter.name != sourceChapter.name ||
            dbChapter.dateUpload != sourceChapter.dateUpload ||
            dbChapter.chapterNumber != sourceChapter.chapterNumber ||
            dbChapter.sourceOrder != sourceChapter.sourceOrder ||
            dbChapter.memo != sourceChapter.memo
    }
}
