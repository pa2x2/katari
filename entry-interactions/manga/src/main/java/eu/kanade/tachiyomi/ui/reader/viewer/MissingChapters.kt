package eu.kanade.tachiyomi.ui.reader.viewer

import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import tachiyomi.domain.entry.service.calculateChapterGap

internal fun calculateChapterGap(higherReaderChapter: ReaderChapter?, lowerReaderChapter: ReaderChapter?): Int {
    val higher = higherReaderChapter?.chapter?.chapter_number?.toDouble() ?: return 0
    val lower = lowerReaderChapter?.chapter?.chapter_number?.toDouble() ?: return 0
    return calculateChapterGap(higher, lower)
}
