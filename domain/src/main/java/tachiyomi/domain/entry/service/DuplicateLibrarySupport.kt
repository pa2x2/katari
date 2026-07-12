package tachiyomi.domain.entry.service

import com.aallam.similarity.NormalizedLevenshtein
import tachiyomi.domain.entry.model.DuplicateMatchReason
import tachiyomi.domain.library.service.DuplicatePreferences
import tachiyomi.domain.library.service.DuplicateTitleExclusions
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class DuplicateEntryMetadata(
    val id: Long,
    val title: String,
    val description: String? = null,
    val primaryCreator: String? = null,
    val secondaryCreator: String? = null,
    val genres: List<String>? = null,
    val status: Long = 0L,
    val count: Long? = null,
)

data class DuplicateLibraryCandidate<T>(
    val item: T,
    val sortTitle: String,
    val memberIds: List<Long>,
    val memberEntries: List<DuplicateEntryMetadata>,
    val count: Long,
    val contentSignature: Long,
)

data class DuplicateLibraryMatch<T>(
    val item: T,
    val count: Long,
    val cheapScore: Int,
    val scoreMax: Int,
    val score: Int,
    val reasons: List<DuplicateMatchReason>,
    val contentSignature: Long,
)

data class DuplicateConfig(
    val extendedEnabled: Boolean,
    val minimumMatchScore: Int,
    val weights: ExtendedWeights,
    val titleExclusionPatterns: List<TitleExclusionPattern>,
)

data class TitleExclusionPattern(
    val pattern: String,
    val regex: Regex,
)

data class ExtendedWeights(
    val description: Int,
    val author: Int,
    val artist: Int,
    val cover: Int,
    val genre: Int,
    val status: Int,
    val chapterCount: Int,
    val title: Int,
)

fun DuplicatePreferences.toDuplicateConfig(): DuplicateConfig {
    return DuplicateConfig(
        extendedEnabled = extendedDuplicateDetectionEnabled.get(),
        minimumMatchScore = minimumMatchScore.get().coerceIn(0, DuplicatePreferences.TOTAL_SCORE_BUDGET),
        weights = getWeightBudget().toExtendedWeights(),
        titleExclusionPatterns = DuplicateTitleExclusions.compilePatterns(titleExclusionPatterns.get())
            .map { TitleExclusionPattern(it.pattern, it.regex) },
    )
}

fun DuplicatePreferences.DuplicateWeightBudget.toExtendedWeights(): ExtendedWeights {
    return ExtendedWeights(
        description = description,
        author = author,
        artist = artist,
        cover = cover,
        genre = genre,
        status = status,
        chapterCount = chapterCount,
        title = title,
    )
}

fun ExtendedWeights.baseScoreMax(): Int {
    return (description + author + artist + genre + status + chapterCount + title).coerceAtLeast(1)
}

object DuplicateLibrarySupport {
    private val normalizedLevenshtein = NormalizedLevenshtein()

    fun <T> detectDuplicates(
        currentEntry: DuplicateEntryMetadata,
        libraryEntries: List<DuplicateLibraryCandidate<T>>,
        excludedIds: Set<Long>,
        trackerDuplicateIds: Set<Long>,
        config: DuplicateConfig,
    ): List<DuplicateLibraryMatch<T>> {
        val current = currentEntry.toPreparedDuplicateEntry(config)
        if (config.extendedEnabled) {
            if (current.normalizedTitle.isBlank()) return emptyList()
        } else if (current.rawLowercaseTitle.isBlank()) {
            return emptyList()
        }

        return libraryEntries.asSequence()
            .filterNot { libraryItem -> libraryItem.memberIds.any { it in excludedIds } }
            .mapNotNull { libraryItem ->
                val match = if (config.extendedEnabled) {
                    scoreExtendedDuplicate(current, libraryItem, trackerDuplicateIds, config)
                } else {
                    scoreLegacyDuplicate(current, libraryItem, trackerDuplicateIds)
                }
                match?.let { libraryItem.sortTitle to it }
            }
            .sortedWith(
                compareByDescending<Pair<String, DuplicateLibraryMatch<T>>> { it.second.score }
                    .thenBy { it.first.lowercase(Locale.ROOT) },
            )
            .map { it.second }
            .toList()
    }

    private fun <T> scoreExtendedDuplicate(
        current: PreparedDuplicateEntry,
        libraryItem: DuplicateLibraryCandidate<T>,
        trackerDuplicateIds: Set<Long>,
        config: DuplicateConfig,
    ): DuplicateLibraryMatch<T>? {
        val weights = config.weights
        val bestMatch = libraryItem.memberEntries.asSequence()
            .map { it.toPreparedDuplicateEntry(config) }
            .mapNotNull { candidate -> scoreExtendedDuplicate(current, candidate, trackerDuplicateIds, config) }
            .maxByOrNull { it.score }
            ?: return null

        return DuplicateLibraryMatch(
            item = libraryItem.item,
            count = libraryItem.count,
            cheapScore = bestMatch.score,
            scoreMax = weights.baseScoreMax(),
            score = if (libraryItem.memberIds.any { it in trackerDuplicateIds }) {
                max(bestMatch.score, LEGACY_TRACKER_MATCH_SCORE)
            } else {
                bestMatch.score
            },
            reasons = if (
                libraryItem.memberIds.any { it in trackerDuplicateIds } &&
                DuplicateMatchReason.TRACKER !in bestMatch.reasons
            ) {
                bestMatch.reasons + DuplicateMatchReason.TRACKER
            } else {
                bestMatch.reasons
            },
            contentSignature = libraryItem.contentSignature,
        )
    }

    private fun scoreExtendedDuplicate(
        current: PreparedDuplicateEntry,
        candidate: PreparedDuplicateEntry,
        trackerDuplicateIds: Set<Long>,
        config: DuplicateConfig,
    ): ScoredDuplicate? {
        val weights = config.weights
        if (current.normalizedTitle.isBlank() || candidate.normalizedTitle.isBlank()) return null

        val titleSimilarity = calculateTitleSimilarity(current, candidate)
        val descriptionSimilarity = calculateDescriptionSimilarity(current, candidate)
        val countSimilarity = calculateCountSimilarity(current.count, candidate.count)

        val reasons = linkedSetOf<DuplicateMatchReason>()
        var score = 0

        if (weights.description > 0 && descriptionSimilarity >= DESCRIPTION_REASON_THRESHOLD) {
            reasons += DuplicateMatchReason.DESCRIPTION
        }
        score += scaledSimilarity(descriptionSimilarity, weights.description)

        if (weights.title > 0 && titleSimilarity >= TITLE_REASON_THRESHOLD) {
            reasons += DuplicateMatchReason.TITLE
        }
        score += scaledSimilarity(titleSimilarity, weights.title)

        val authorMatched =
            current.primaryCreator != null && candidate.creators.any { namesMatch(current.primaryCreator, it) }
        val artistMatched =
            current.secondaryCreator != null && candidate.creators.any { namesMatch(current.secondaryCreator, it) }

        if (weights.author > 0 && authorMatched) {
            reasons += DuplicateMatchReason.AUTHOR
            score += weights.author
        }
        if (weights.artist > 0 && artistMatched) {
            reasons += DuplicateMatchReason.ARTIST
            score += weights.artist
        }
        if (current.creators.isNotEmpty() && candidate.creators.isNotEmpty() && !authorMatched && !artistMatched) {
            score -= CREATOR_MISMATCH_PENALTY
        }

        when {
            weights.status > 0 && hasKnownMatchingStatus(current.status, candidate.status) -> {
                reasons += DuplicateMatchReason.STATUS
                score += weights.status
            }
            hasKnownConflictingStatus(current.status, candidate.status) -> {
                score -= STATUS_MISMATCH_PENALTY
            }
        }

        val genreOverlap = current.genres.intersect(candidate.genres).size
        if (weights.genre > 0 && genreOverlap > 0) {
            reasons += DuplicateMatchReason.GENRE
            score += scaledGenreWeight(genreOverlap, weights.genre)
        }

        if (weights.chapterCount > 0 && countSimilarity >= COUNT_REASON_THRESHOLD) {
            reasons += DuplicateMatchReason.CHAPTER_COUNT
        }
        score += scaledSimilarity(countSimilarity, weights.chapterCount)

        if (candidate.id in trackerDuplicateIds) {
            reasons += DuplicateMatchReason.TRACKER
            score = max(score, LEGACY_TRACKER_MATCH_SCORE)
        }

        if (reasons.isEmpty()) return null

        score = score.coerceIn(0, DuplicatePreferences.TOTAL_SCORE_BUDGET)
        if (score < config.minimumMatchScore && DuplicateMatchReason.TRACKER !in reasons) return null

        val hasCreatorMatch = authorMatched || artistMatched
        val hasStrongNonTitleEvidence =
            descriptionSimilarity >= STRONG_DESCRIPTION_THRESHOLD ||
                (hasCreatorMatch && descriptionSimilarity >= SUPPORTING_DESCRIPTION_THRESHOLD) ||
                (hasCreatorMatch && genreOverlap >= 2) ||
                (descriptionSimilarity >= MEDIUM_DESCRIPTION_THRESHOLD && genreOverlap >= 2) ||
                (
                    descriptionSimilarity >= SUPPORTING_DESCRIPTION_THRESHOLD &&
                        countSimilarity >= COUNT_REASON_THRESHOLD
                    ) ||
                (countSimilarity >= COUNT_REASON_THRESHOLD && genreOverlap >= 2) ||
                DuplicateMatchReason.TRACKER in reasons

        val hasSupportingTitleEvidence =
            titleSimilarity >= TITLE_REASON_THRESHOLD && (authorMatched || artistMatched || genreOverlap >= 1)
        val hasStrongTitleEvidence = titleSimilarity >= STRONG_TITLE_THRESHOLD || hasSupportingTitleEvidence
        if (!hasStrongNonTitleEvidence && !hasStrongTitleEvidence) {
            return null
        }

        return ScoredDuplicate(
            score = score,
            reasons = reasons.toList(),
        )
    }

    private fun <T> scoreLegacyDuplicate(
        current: PreparedDuplicateEntry,
        libraryItem: DuplicateLibraryCandidate<T>,
        trackerDuplicateIds: Set<Long>,
    ): DuplicateLibraryMatch<T>? {
        val reasons = linkedSetOf<DuplicateMatchReason>()

        val titleMatched = libraryItem.memberEntries.any { candidate ->
            candidate.title.lowercase(Locale.ROOT).contains(current.rawLowercaseTitle)
        }
        if (titleMatched) {
            reasons += DuplicateMatchReason.TITLE
        }

        val trackerMatched = libraryItem.memberIds.any { it in trackerDuplicateIds }
        if (trackerMatched) {
            reasons += DuplicateMatchReason.TRACKER
        }

        if (reasons.isEmpty()) return null

        val score = when {
            trackerMatched && titleMatched -> LEGACY_TITLE_MATCH_SCORE.coerceAtLeast(LEGACY_TRACKER_MATCH_SCORE)
            trackerMatched -> LEGACY_TRACKER_MATCH_SCORE
            else -> LEGACY_TITLE_MATCH_SCORE
        }

        return DuplicateLibraryMatch(
            item = libraryItem.item,
            count = libraryItem.count,
            cheapScore = score,
            scoreMax = LEGACY_SCORE_MAX,
            score = score,
            reasons = reasons.toList(),
            contentSignature = libraryItem.contentSignature,
        )
    }

    private fun calculateTitleSimilarity(
        current: PreparedDuplicateEntry,
        candidate: PreparedDuplicateEntry,
    ): Double {
        if (current.normalizedTitle == candidate.normalizedTitle) return 1.0

        val levenshtein = normalizedLevenshtein.similarity(current.normalizedTitle, candidate.normalizedTitle)

        val sharedTokens = current.titleTokens.intersect(candidate.titleTokens)
        val meaningfulSharedTokens = sharedTokens.filterNot(::isCommonTitleToken).toSet()
        val subsetCoverage = if (sharedTokens.size >= MIN_SHARED_TITLE_TOKENS) {
            meaningfulSharedTokens.size.toDouble() / min(current.titleTokens.size, candidate.titleTokens.size)
        } else {
            0.0
        }

        val containsSimilarity = if (
            min(current.normalizedTitle.length, candidate.normalizedTitle.length) >= CONTAINS_MIN_TITLE_LENGTH &&
            (
                current.normalizedTitle.contains(candidate.normalizedTitle) ||
                    candidate.normalizedTitle.contains(current.normalizedTitle)
                )
        ) {
            max(
                min(current.normalizedTitle.length, candidate.normalizedTitle.length).toDouble() /
                    max(current.normalizedTitle.length, candidate.normalizedTitle.length),
                if (min(current.titleTokens.size, candidate.titleTokens.size) == 1) {
                    SINGLE_TOKEN_CONTAINS_SIMILARITY
                } else {
                    0.0
                },
            )
        } else {
            0.0
        }

        return max(levenshtein, max(subsetCoverage, containsSimilarity))
    }

    private fun calculateDescriptionSimilarity(
        current: PreparedDuplicateEntry,
        candidate: PreparedDuplicateEntry,
    ): Double {
        val currentDescription = current.normalizedDescription ?: return 0.0
        val candidateDescription = candidate.normalizedDescription ?: return 0.0

        val levenshtein = normalizedLevenshtein.similarity(currentDescription, candidateDescription)
        val sharedTokens = current.descriptionTokens.intersect(candidate.descriptionTokens)
        val overlap = if (
            sharedTokens.isNotEmpty() &&
            current.descriptionTokens.isNotEmpty() &&
            candidate.descriptionTokens.isNotEmpty()
        ) {
            sharedTokens.size.toDouble() / min(current.descriptionTokens.size, candidate.descriptionTokens.size)
        } else {
            0.0
        }

        return max(levenshtein, overlap)
    }

    private fun calculateCountSimilarity(
        currentCount: Long?,
        candidateCount: Long?,
    ): Double {
        if (currentCount == null || candidateCount == null) return 0.0
        if (currentCount <= 0 || candidateCount <= 0) return 0.0

        val diff = abs(currentCount - candidateCount)
        val maxCount = max(currentCount, candidateCount)
        return (1.0 - (diff.toDouble() / maxCount.toDouble())).coerceIn(0.0, 1.0)
    }

    private fun namesMatch(
        current: String,
        candidate: String,
    ): Boolean {
        return current == candidate ||
            (
                min(current.length, candidate.length) >= CONTAINS_MIN_TITLE_LENGTH &&
                    (current.contains(candidate) || candidate.contains(current))
                ) ||
            normalizedLevenshtein.similarity(current, candidate) >= MIN_NAME_SIMILARITY
    }

    private fun hasKnownMatchingStatus(
        currentStatus: Long,
        candidateStatus: Long,
    ): Boolean {
        return currentStatus != 0L && candidateStatus != 0L && currentStatus == candidateStatus
    }

    private fun hasKnownConflictingStatus(
        currentStatus: Long,
        candidateStatus: Long,
    ): Boolean {
        return currentStatus != 0L && candidateStatus != 0L && currentStatus != candidateStatus
    }

    private fun DuplicateEntryMetadata.toPreparedDuplicateEntry(
        config: DuplicateConfig,
    ): PreparedDuplicateEntry {
        val normalizedTitle = normalizeTitle(title, config.titleExclusionPatterns)
        val normalizedDescription = normalizeDescription(description)
        return PreparedDuplicateEntry(
            id = id,
            normalizedTitle = normalizedTitle,
            rawLowercaseTitle = title.lowercase(Locale.ROOT).trim(),
            titleTokens = normalizedTitle.split(' ')
                .asSequence()
                .filter { it.length >= MIN_TITLE_TOKEN_LENGTH }
                .toSet(),
            normalizedDescription = normalizedDescription,
            descriptionTokens = normalizedDescription
                ?.split(' ')
                ?.asSequence()
                ?.filter { it.length >= MIN_DESCRIPTION_TOKEN_LENGTH }
                ?.toSet()
                .orEmpty(),
            primaryCreator = normalizeValue(primaryCreator),
            secondaryCreator = normalizeValue(secondaryCreator),
            genres = genres.orEmpty()
                .mapNotNull(::normalizeValue)
                .toSet(),
            status = status,
            count = count,
        )
    }

    private fun normalizeTitle(
        title: String,
        titleExclusionPatterns: List<TitleExclusionPattern>,
    ): String {
        return applyTitleExclusions(title, titleExclusionPatterns)
            .lowercase(Locale.ROOT)
            .replace(CHAPTER_REFERENCE_REGEX, " ")
            .replace(NON_TEXT_REGEX, " ")
            .replace(CONSECUTIVE_SPACES_REGEX, " ")
            .trim()
    }

    private fun applyTitleExclusions(
        title: String,
        titleExclusionPatterns: List<TitleExclusionPattern>,
    ): String {
        return titleExclusionPatterns.fold(title) { current, pattern ->
            current.replace(pattern.regex, " ")
        }
    }

    private fun normalizeValue(value: String?): String? {
        return value
            ?.lowercase(Locale.ROOT)
            ?.replace(NON_TEXT_REGEX, " ")
            ?.replace(CONSECUTIVE_SPACES_REGEX, " ")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun normalizeDescription(description: String?): String? {
        return description
            ?.lowercase(Locale.ROOT)
            ?.replace(DESCRIPTION_NOISE_REGEX, " ")
            ?.replace(NON_TEXT_REGEX, " ")
            ?.replace(CONSECUTIVE_SPACES_REGEX, " ")
            ?.trim()
            ?.takeIf { it.length >= MIN_DESCRIPTION_LENGTH }
    }

    private fun isCommonTitleToken(token: String): Boolean {
        return token in COMMON_TITLE_TOKENS
    }

    private fun scaledSimilarity(similarity: Double, weight: Int): Int {
        return (similarity * weight).roundToInt()
    }

    private fun scaledGenreWeight(genreOverlap: Int, weight: Int): Int {
        if (weight <= 0 || genreOverlap <= 0) return 0
        val overlapScale = min(genreOverlap, MAX_GENRE_MATCH_COUNT).toDouble() / MAX_GENRE_MATCH_COUNT.toDouble()
        return (weight * overlapScale).roundToInt().coerceAtLeast(1)
    }

    private data class PreparedDuplicateEntry(
        val id: Long,
        val normalizedTitle: String,
        val rawLowercaseTitle: String,
        val titleTokens: Set<String>,
        val normalizedDescription: String?,
        val descriptionTokens: Set<String>,
        val primaryCreator: String?,
        val secondaryCreator: String?,
        val genres: Set<String>,
        val status: Long,
        val count: Long?,
    ) {
        val creators: Set<String> = listOfNotNull(primaryCreator, secondaryCreator).toSet()
    }

    private data class ScoredDuplicate(
        val score: Int,
        val reasons: List<DuplicateMatchReason>,
    )

    private const val CREATOR_MISMATCH_PENALTY = 10
    private const val STATUS_MISMATCH_PENALTY = 2
    private const val LEGACY_TITLE_MATCH_SCORE = 46
    private const val LEGACY_TRACKER_MATCH_SCORE = 58
    private const val LEGACY_SCORE_MAX = 100
    private const val MIN_NAME_SIMILARITY = 0.9
    private const val CONTAINS_MIN_TITLE_LENGTH = 6
    private const val MIN_SHARED_TITLE_TOKENS = 2
    private const val MIN_TITLE_TOKEN_LENGTH = 2
    private const val MIN_DESCRIPTION_TOKEN_LENGTH = 4
    private const val MIN_DESCRIPTION_LENGTH = 48
    private const val TITLE_REASON_THRESHOLD = 0.45
    private const val DESCRIPTION_REASON_THRESHOLD = 0.35
    private const val COUNT_REASON_THRESHOLD = 0.95
    private const val STRONG_TITLE_THRESHOLD = 0.85
    private const val STRONG_DESCRIPTION_THRESHOLD = 0.62
    private const val MEDIUM_DESCRIPTION_THRESHOLD = 0.5
    private const val SUPPORTING_DESCRIPTION_THRESHOLD = 0.35
    private const val SINGLE_TOKEN_CONTAINS_SIMILARITY = 0.78
    private const val MAX_GENRE_MATCH_COUNT = 4

    private val COMMON_TITLE_TOKENS = setOf(
        "a",
        "an",
        "and",
        "at",
        "for",
        "from",
        "in",
        "of",
        "on",
        "or",
        "the",
        "to",
        "with",
    )

    private val NON_TEXT_REGEX = Regex("[^\\p{L}0-9 ]")
    private val CONSECUTIVE_SPACES_REGEX = Regex(" +")
    private val CHAPTER_REFERENCE_REGEX = Regex("""((- часть|- глава) \d*)""")
    private val DESCRIPTION_NOISE_REGEX = Regex("""\b(chapter|chapters|read|summary|synopsis|description)\b""")
}
