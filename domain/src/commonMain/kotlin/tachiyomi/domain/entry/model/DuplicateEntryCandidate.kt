package tachiyomi.domain.entry.model

import androidx.compose.runtime.Immutable

@Immutable
data class DuplicateEntryCandidate(
    val entry: Entry,
    val count: Long,
    val cheapScore: Int,
    val scoreMax: Int,
    val score: Int,
    val reasons: List<DuplicateMatchReason>,
    val contentSignature: Long = entry.lastModifiedAt,
) {
    val scorePercent: Int
        get() = if (scoreMax <= 0) 0 else ((score.toDouble() / scoreMax.toDouble()) * 100).toInt().coerceIn(0, 100)

    val isStrongMatch: Boolean
        get() = scorePercent >= STRONG_MATCH_PERCENT

    companion object {
        const val STRONG_MATCH_PERCENT = 82
    }
}
