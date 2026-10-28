package mihon.feature.migration.discovery

import com.aallam.similarity.NormalizedLevenshtein
import mihon.feature.migration.discovery.model.SourceMigrationDiscoveredCandidate
import mihon.feature.migration.session.model.SourceMigrationMatchKind
import tachiyomi.domain.entry.model.Entry

internal class SourceMigrationCandidateRanker(
    private val queryFactory: SourceMigrationSearchQueryFactory,
) {
    private val similarity = NormalizedLevenshtein()

    fun rank(
        sourceTitle: String,
        entriesBySourcePriority: List<Pair<Int, Entry>>,
    ): List<SourceMigrationDiscoveredCandidate> {
        val normalizedSourceTitle = queryFactory.normalize(sourceTitle)
        return entriesBySourcePriority
            .mapNotNull { (sourcePriority, entry) ->
                val normalizedCandidateTitle = queryFactory.normalize(entry.title)
                val score = when {
                    normalizedSourceTitle.isEmpty() || normalizedCandidateTitle.isEmpty() -> 0.0
                    else -> similarity.similarity(normalizedSourceTitle, normalizedCandidateTitle)
                }
                if (score < MIN_ELIGIBLE_SCORE) return@mapNotNull null
                SourceMigrationDiscoveredCandidate(
                    entry = entry,
                    sourcePriority = sourcePriority,
                    score = score,
                    matchKind = if (normalizedSourceTitle == normalizedCandidateTitle) {
                        SourceMigrationMatchKind.EXACT
                    } else {
                        SourceMigrationMatchKind.SIMILAR
                    },
                )
            }
            .groupBy { it.entry.source to it.entry.url }
            .mapNotNull { (_, duplicates) -> duplicates.maxByOrNull(SourceMigrationDiscoveredCandidate::score) }
            .sortedWith(
                compareBy<SourceMigrationDiscoveredCandidate> { it.matchKind != SourceMigrationMatchKind.EXACT }
                    .thenBy(SourceMigrationDiscoveredCandidate::sourcePriority)
                    .thenByDescending(SourceMigrationDiscoveredCandidate::score)
                    .thenBy { it.entry.title.lowercase() },
            )
            .take(MAX_CANDIDATES)
    }

    private companion object {
        const val MIN_ELIGIBLE_SCORE = 0.4
        const val MAX_CANDIDATES = 20
    }
}
