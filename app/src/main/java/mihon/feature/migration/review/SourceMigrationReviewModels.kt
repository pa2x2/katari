package mihon.feature.migration.review

import mihon.feature.migration.session.model.SourceMigrationItemState
import mihon.feature.migration.session.model.SourceMigrationSession
import mihon.feature.migration.session.model.SourceMigrationSessionGroupMember
import mihon.feature.migration.session.model.SourceMigrationSessionItem

internal enum class SourceMigrationReviewFilter {
    ALL,
    FOUND,
    SEARCHING,
    NEEDS_REVIEW,
    NO_MATCH,
}

internal data class SourceMigrationReviewGroup(
    val id: Long,
    val visibleEntryId: Long,
    val title: String,
    val memberCount: Int,
    val mappings: List<SourceMigrationReviewMapping>,
    val notSelectedMembers: List<SourceMigrationReviewMember>,
) {
    val readyMappings: List<SourceMigrationReviewMapping>
        get() = mappings.filter { it.item.state == SourceMigrationItemState.READY }

    val allReadyMappingsIncluded: Boolean
        get() = readyMappings.isNotEmpty() && readyMappings.all { it.item.included }
}

internal data class SourceMigrationReviewMapping(
    val item: SourceMigrationSessionItem,
    val sourceName: String,
    val targetSourceName: String?,
)

internal data class SourceMigrationReviewMember(
    val member: SourceMigrationSessionGroupMember,
    val sourceName: String,
)

internal data class SourceMigrationReviewState(
    val isLoaded: Boolean = false,
    val session: SourceMigrationSession? = null,
    val originSourceName: String = "",
    val groups: List<SourceMigrationReviewGroup> = emptyList(),
    val filter: SourceMigrationReviewFilter = SourceMigrationReviewFilter.ALL,
    val actionInProgress: Boolean = false,
    val discardCompleted: Boolean = false,
) {
    val visibleGroups: List<SourceMigrationReviewGroup>
        get() = groups.mapNotNull { group ->
            when (filter) {
                SourceMigrationReviewFilter.ALL -> group
                SourceMigrationReviewFilter.FOUND -> group.filtered { mapping ->
                    mapping.item.targetTitle != null && mapping.item.state !in SEARCHING_STATES
                }
                SourceMigrationReviewFilter.SEARCHING -> group.filtered { mapping ->
                    mapping.item.state in SEARCHING_STATES
                }
                SourceMigrationReviewFilter.NEEDS_REVIEW -> group.filtered { mapping ->
                    mapping.item.state in REVIEW_STATES
                }
                SourceMigrationReviewFilter.NO_MATCH -> group.filtered { mapping ->
                    mapping.item.targetTitle == null && mapping.item.state !in SEARCHING_STATES
                }
            }
        }

    val foundCount: Int
        get() = groups.sumOf { group ->
            group.mappings.count { mapping ->
                mapping.item.targetTitle != null && mapping.item.state !in SEARCHING_STATES
            }
        }

    val searchingCount: Int
        get() = groups.sumOf { group ->
            group.mappings.count { mapping -> mapping.item.state in SEARCHING_STATES }
        }

    val needsReviewCount: Int
        get() = groups.sumOf { group -> group.mappings.count { it.item.state in REVIEW_STATES } }

    val noMatchCount: Int
        get() = groups.sumOf { group ->
            group.mappings.count { mapping ->
                mapping.item.targetTitle == null && mapping.item.state !in SEARCHING_STATES
            }
        }

    private fun SourceMigrationReviewGroup.filtered(
        predicate: (SourceMigrationReviewMapping) -> Boolean,
    ): SourceMigrationReviewGroup? {
        val filteredMappings = mappings.filter(predicate)
        return copy(
            mappings = filteredMappings,
            notSelectedMembers = emptyList(),
        ).takeIf { filteredMappings.isNotEmpty() }
    }

    private companion object {
        val SEARCHING_STATES = setOf(
            SourceMigrationItemState.DISCOVERY_QUEUED,
            SourceMigrationItemState.DISCOVERING,
        )

        val REVIEW_STATES = setOf(
            SourceMigrationItemState.NEEDS_REVIEW,
            SourceMigrationItemState.DISCOVERY_FAILED,
            SourceMigrationItemState.CONFLICT,
            SourceMigrationItemState.EXECUTION_FAILED,
            SourceMigrationItemState.APPLIED_INCOMPLETE,
        )
    }
}
