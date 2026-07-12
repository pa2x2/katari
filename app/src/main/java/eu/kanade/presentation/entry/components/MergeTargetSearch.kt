package eu.kanade.presentation.entry.components

import com.aallam.similarity.NormalizedLevenshtein
import tachiyomi.domain.library.service.DuplicatePreferences
import tachiyomi.domain.library.service.DuplicateTitleExclusions
import java.util.Locale
import kotlin.math.max

private val normalizedLevenshtein = NormalizedLevenshtein()
private val nonTextRegex = Regex("[^\\p{L}0-9 ]")
private val consecutiveSpacesRegex = Regex("\\s+")
private val aliasSeparatorRegex = Regex("\\s*/\\s*")

private const val MIN_ELIGIBLE_SCORE = 0.56
private const val MIN_SEEDED_QUERY_LENGTH = 3
private const val MIN_TOKEN_SIMILARITY = 0.86
private const val MIN_COMPARABLE_TOKEN_LENGTH = 3
private const val MIN_SINGLE_TOKEN_LENGTH = 4

interface MergeSearchTarget {
    val mergeSearchTitle: String
    val mergeSearchableTitle: String
}

fun buildMergeTargetQuery(
    title: String,
    duplicatePreferences: DuplicatePreferences,
): String {
    val trimmed = title.trim()
    if (trimmed.isBlank()) return title

    val stripped = DuplicateTitleExclusions.compilePatterns(duplicatePreferences.titleExclusionPatterns.get())
        .fold(trimmed) { current, pattern ->
            current.replace(pattern.regex, " ")
        }
        .replace(consecutiveSpacesRegex, " ")
        .trim()

    return stripped.takeIf { it.length >= MIN_SEEDED_QUERY_LENGTH } ?: trimmed
}

fun <T : MergeSearchTarget> rankMergeTargets(
    targets: Iterable<T>,
    query: String,
): List<T> {
    val trimmed = query.trim()
    if (trimmed.isBlank()) return targets.toList()

    val queryVariants = buildQueryVariants(trimmed)
    if (queryVariants.isEmpty()) return targets.toList()

    return targets.asSequence()
        .map { target -> target to target.searchScore(queryVariants) }
        .filter { (_, score) -> score >= MIN_ELIGIBLE_SCORE }
        .sortedWith(
            compareByDescending<Pair<T, Double>> { it.second }
                .thenBy { it.first.mergeSearchTitle.lowercase(Locale.ROOT) },
        )
        .map { it.first }
        .toList()
}

fun MergeTarget.matchesQuery(query: String): Boolean {
    return rankMergeTargets(listOf(this), query).isNotEmpty()
}

internal fun normalizeForSearch(value: String): String {
    return value.lowercase(Locale.ROOT)
        .replace(nonTextRegex, " ")
        .replace(consecutiveSpacesRegex, " ")
        .trim()
}

private fun MergeSearchTarget.searchScore(queryVariants: List<String>): Double {
    val title = normalizeForSearch(mergeSearchTitle)
    val searchable = normalizeForSearch(mergeSearchableTitle)
    val haystacks = buildList {
        if (title.isNotBlank()) add(title)
        if (searchable.isNotBlank() && searchable != title) add(searchable)
    }

    return haystacks.maxOfOrNull { haystack ->
        scoreQueryVariants(queryVariants, haystack)
    } ?: 0.0
}

private fun scoreQueryVariants(queryVariants: List<String>, haystack: String): Double {
    val best = queryVariants.maxOf { variant -> score(variant, haystack) }
    val combined = queryVariants.filter { it.contains(' ') }.maxOfOrNull { variant -> score(variant, haystack) } ?: 0.0
    return max(best, combined)
}

private fun score(query: String, haystack: String): Double {
    if (haystack == query) return 1.0
    if (haystack.contains(query)) {
        val lengthRatio = query.length.toDouble() / haystack.length.toDouble().coerceAtLeast(1.0)
        return 0.96 + (0.03 * lengthRatio)
    }

    val queryTokens = query.split(' ').filter { it.isNotBlank() }
    val haystackTokens = haystack.split(' ').filter { it.isNotBlank() }
    if (queryTokens.isEmpty() || haystackTokens.isEmpty()) return 0.0

    val significantQueryTokens = queryTokens.filter { it.length >= MIN_SINGLE_TOKEN_LENGTH }
    if (significantQueryTokens.isNotEmpty() && significantQueryTokens.none { queryToken ->
            haystackTokens.any { token ->
                isStrongTokenMatch(queryToken, token)
            }
        }
    ) {
        return 0.0
    }

    val matchedTokens = queryTokens.count { queryToken ->
        haystackTokens.any { token ->
            isStrongTokenMatch(queryToken, token)
        }
    }
    val tokenCoverage = matchedTokens.toDouble() / queryTokens.size.toDouble()
    if (tokenCoverage == 0.0) return 0.0
    if (queryTokens.size > 1 && tokenCoverage < 0.5) return 0.0

    val containsSimilarity = queryTokens.maxOf { queryToken ->
        haystackTokens.maxOf { token ->
            when {
                token == queryToken -> 1.0
                token.contains(queryToken) || queryToken.contains(token) -> {
                    minOf(token.length, queryToken.length).toDouble() /
                        max(token.length, queryToken.length).toDouble()
                }
                else -> 0.0
            }
        }
    }
    val levenshtein = if (queryTokens.size == 1) {
        normalizedLevenshtein.similarity(query, haystack) * 0.6
    } else {
        0.0
    }

    return max(levenshtein, max(tokenCoverage, containsSimilarity * 0.94))
}

private fun buildQueryVariants(query: String): List<String> {
    val normalized = normalizeForSearch(query)
    if (normalized.isBlank()) return emptyList()

    val aliases = query.split(aliasSeparatorRegex)
        .map(::normalizeForSearch)
        .filter { it.length >= MIN_SEEDED_QUERY_LENGTH }

    return if (aliases.size > 1) {
        aliases.distinct()
    } else {
        listOf(normalized)
    }
}

private fun isStrongTokenMatch(queryToken: String, token: String): Boolean {
    if (queryToken == token) return true
    if (minOf(queryToken.length, token.length) < MIN_COMPARABLE_TOKEN_LENGTH) return false

    return token.contains(queryToken) ||
        queryToken.contains(token) ||
        token.startsWith(queryToken) ||
        normalizedLevenshtein.similarity(queryToken, token) >= MIN_TOKEN_SIMILARITY
}
