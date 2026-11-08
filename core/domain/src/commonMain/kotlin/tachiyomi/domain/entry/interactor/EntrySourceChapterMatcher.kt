package tachiyomi.domain.entry.interactor

import tachiyomi.domain.entry.model.EntryChapter
import kotlin.math.abs

internal class EntrySourceChapterMatcher(
    private val chapters: List<EntryChapter>,
    currentSourceUrls: Set<String>,
    currentSourceNames: Set<String>,
) {
    private val unmatchedChapterIds = chapters.mapTo(mutableSetOf(), EntryChapter::id)

    private val chaptersByUrl = chapters.groupBy(EntryChapter::url)

    private val chaptersByNameAndNumber by lazy(LazyThreadSafetyMode.NONE) {
        chapters.mapNotNull { chapter ->
            chapterNumberKey(chapter.chapterNumber)?.let { chapterNumber ->
                NameAndNumber(chapter.name, chapterNumber) to chapter
            }
        }.groupBy(
            keySelector = Pair<NameAndNumber, EntryChapter>::first,
            valueTransform = Pair<NameAndNumber, EntryChapter>::second,
        )
    }

    private val chaptersByName by lazy(LazyThreadSafetyMode.NONE) {
        chapters.groupBy(EntryChapter::name)
    }

    private val staleChaptersByNumber by lazy(LazyThreadSafetyMode.NONE) {
        chapters.mapNotNull { chapter ->
            if (chapter.url in currentSourceUrls || chapter.name in currentSourceNames) {
                return@mapNotNull null
            }
            chapterNumberKey(chapter.chapterNumber)?.let { chapterNumber -> chapterNumber to chapter }
        }.groupBy(
            keySelector = Pair<Double, EntryChapter>::first,
            valueTransform = Pair<Double, EntryChapter>::second,
        )
    }

    private val chaptersBySourceOrder by lazy(LazyThreadSafetyMode.NONE) {
        chapters.groupBy(EntryChapter::sourceOrder)
    }

    fun match(
        sourceUrl: String,
        resolvedName: String,
        chapterNumber: Double,
        sourceOrder: Long,
    ): EntryChapter? {
        val chapterNumberKey = chapterNumberKey(chapterNumber)
        return match(chaptersByUrl[sourceUrl], sourceOrder)
            ?: chapterNumberKey?.let { number ->
                match(chaptersByNameAndNumber[NameAndNumber(resolvedName, number)], sourceOrder)
            }
            ?: match(chaptersByName[resolvedName], sourceOrder)
            ?: if (chapterNumber >= 0.0 && chapterNumberKey != null) {
                match(staleChaptersByNumber[chapterNumberKey], sourceOrder)
            } else {
                null
            }
            ?: if (chapterNumber < 0.0 && resolvedName == sourceUrl) {
                match(chaptersBySourceOrder[sourceOrder], sourceOrder)
            } else {
                null
            }
    }

    private fun match(candidates: List<EntryChapter>?, sourceOrder: Long): EntryChapter? {
        var nearestCandidate: EntryChapter? = null
        var nearestDistance = Long.MAX_VALUE
        candidates?.forEach { candidate ->
            if (candidate.id !in unmatchedChapterIds) return@forEach
            if (candidate.sourceOrder == sourceOrder) {
                unmatchedChapterIds -= candidate.id
                return candidate
            }

            val distance = abs(candidate.sourceOrder - sourceOrder)
            if (distance < nearestDistance) {
                nearestCandidate = candidate
                nearestDistance = distance
            }
        }

        nearestCandidate?.let { unmatchedChapterIds -= it.id }
        return nearestCandidate
    }

    private fun chapterNumberKey(chapterNumber: Double): Double? {
        return when {
            chapterNumber.isNaN() -> null
            chapterNumber == 0.0 -> 0.0
            else -> chapterNumber
        }
    }

    private data class NameAndNumber(
        val name: String,
        val chapterNumber: Double,
    )
}
