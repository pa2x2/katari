package mihon.feature.migration.discovery.model

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.feature.migration.session.model.SourceMigrationDiscoveryFailureReason
import mihon.feature.migration.session.model.SourceMigrationMatchKind
import tachiyomi.domain.entry.model.Entry

data class SourceMigrationDiscoveryRequest(
    val sourceTitle: String,
    val entryType: EntryType,
    val targetSourceIds: List<Long>,
    val depth: SourceMigrationSearchDepth = SourceMigrationSearchDepth.STANDARD,
) {
    init {
        require(sourceTitle.isNotBlank()) { "Migration discovery title must not be blank" }
        require(targetSourceIds.isNotEmpty()) { "Migration discovery requires target sources" }
        require(targetSourceIds.distinct().size == targetSourceIds.size) {
            "Migration discovery target sources must be unique"
        }
    }
}

enum class SourceMigrationSearchDepth {
    STANDARD,
    BROAD,
}

data class SourceMigrationDiscoveredCandidate(
    val entry: Entry,
    val sourcePriority: Int,
    val score: Double,
    val matchKind: SourceMigrationMatchKind,
)

data class SourceMigrationDiscoveryResult(
    val candidates: List<SourceMigrationDiscoveredCandidate>,
    val failures: List<SourceMigrationSourceFailure>,
)

data class SourceMigrationSourceFailure(
    val sourceId: Long,
    val reason: SourceMigrationDiscoveryFailureReason,
    val retryable: Boolean,
    val detail: String? = null,
)
