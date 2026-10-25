package tachiyomi.domain.entry.service

import tachiyomi.domain.entry.model.EntryChapter
import kotlin.math.floor

/** [fromChapterOne] counts the chapters before the lowest as missing too; a later volume's numbering needn't start at 1. */
fun List<Double>.missingChaptersCount(fromChapterOne: Boolean = true): Int {
    if (this.isEmpty()) {
        return 0
    }

    val chapters = this
        // Ignore unknown chapter numbers
        .filterNot { it == -1.0 }
        // Convert to integers, as we cannot check if 16.5 is missing
        .map(Double::toInt)
        // Only keep unique chapters so that -1 or 16 are not counted multiple times
        .distinct()
        .sorted()

    if (chapters.isEmpty()) {
        return 0
    }

    var missingChaptersCount = 0
    var previousChapter = if (fromChapterOne) 0 else chapters.first() // The actual chapter number, not the array index

    // We go from 0 to lastChapter - Make sure to use the current index instead of the value
    for (i in chapters.indices) {
        val currentChapter = chapters[i]
        if (currentChapter > previousChapter + 1) {
            // Add the amount of missing chapters
            missingChaptersCount += currentChapter - previousChapter - 1
        }
        previousChapter = currentChapter
    }

    return missingChaptersCount
}

/**
 * Missing chapters of one entry's [this] chapters, counted within each group so a volume that restarts its numbering
 * adds no gap. Only the group read first counts the chapters before its lowest.
 */
fun List<EntryChapter>.missingChaptersCountByGroup(): Int {
    return groupSectionsInReadingOrder(compareBy(EntryChapter::chapterNumber))
        .withIndex()
        .sumOf { (index, section) ->
            section.chapters.map(EntryChapter::chapterNumber).missingChaptersCount(fromChapterOne = index == 0)
        }
}

fun calculateChapterGap(higherChapter: EntryChapter?, lowerChapter: EntryChapter?): Int {
    if (higherChapter == null || lowerChapter == null) return 0
    if (!higherChapter.isRecognizedNumber || !lowerChapter.isRecognizedNumber) return 0
    return calculateChapterGap(higherChapter.chapterNumber, lowerChapter.chapterNumber)
}

fun calculateChapterGap(higherChapterNumber: Double, lowerChapterNumber: Double): Int {
    if (higherChapterNumber < 0.0 || lowerChapterNumber < 0.0) return 0
    return floor(higherChapterNumber).toInt() - floor(lowerChapterNumber).toInt() - 1
}
