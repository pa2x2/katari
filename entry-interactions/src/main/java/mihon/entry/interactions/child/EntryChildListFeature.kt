package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.documentation.EntryContentTypeReferenceSection
import mihon.entry.interactions.documentation.entryContentTypeReferenceContribution
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.allOf
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal val ENTRY_CHILD_LIST_FEATURE_ID = FeatureId("entry.child-list")
private val ENTRY_CHILD_LIST_FEATURE_OWNER = ContributionOwner("entry-child-list")
private val ENTRY_CHILD_PROGRESS_REFERENCE = entryContentTypeReferenceContribution(
    id = "partial-progress",
    owner = ENTRY_CHILD_LIST_FEATURE_OWNER,
    section = EntryContentTypeReferenceSection.ENTRY_INTERACTIONS,
    label = "Show partial progress for individual child items",
    order = 300,
)
private val ENTRY_MISSING_CHILD_GAP_REFERENCE = entryContentTypeReferenceContribution(
    id = "missing-child-gaps",
    owner = ENTRY_CHILD_LIST_FEATURE_OWNER,
    section = EntryContentTypeReferenceSection.ENTRY_INTERACTIONS,
    label = "Show gaps between missing child items",
    order = 700,
)
internal val ENTRY_CHILD_LIST_INTEGRATION_ID = FeatureIntegrationId("entry.child-list.provider")
private val ENTRY_CHILD_PROGRESS_INTEGRATION_ID = FeatureIntegrationId("entry.child-list.progress")
private val ENTRY_MISSING_CHILD_GAP_INTEGRATION_ID = FeatureIntegrationId("entry.child-list.missing-gaps")
private val ENTRY_CHILD_LIST_ORDER_BEHAVIOR_ID = FeatureArtifactId("entry.child-list.order")
private val ENTRY_CHILD_LIST_FIRST_CHILD_BEHAVIOR_ID = FeatureArtifactId("entry.child-list.first-reading-child")
private val ENTRY_CHILD_LIST_DISPLAY_BEHAVIOR_ID = FeatureArtifactId("entry.child-list.display")
private val ENTRY_CHILD_PROGRESS_BEHAVIOR_ID = FeatureArtifactId("entry.child-list.progress-labels")
private val ENTRY_MISSING_CHILD_GAP_BEHAVIOR_ID = FeatureArtifactId("entry.child-list.missing-gap-display")

private object EntryChildListOrderBehavior : FeatureBehaviorProjection {
    override val id = ENTRY_CHILD_LIST_ORDER_BEHAVIOR_ID
}

private object EntryChildListDisplayBehavior : FeatureBehaviorProjection {
    override val id = ENTRY_CHILD_LIST_DISPLAY_BEHAVIOR_ID
}

private object EntryChildListFirstChildBehavior : FeatureBehaviorProjection {
    override val id = ENTRY_CHILD_LIST_FIRST_CHILD_BEHAVIOR_ID
}

private object EntryChildProgressBehavior : FeatureBehaviorProjection {
    override val id = ENTRY_CHILD_PROGRESS_BEHAVIOR_ID
}

private object EntryMissingChildGapBehavior : FeatureBehaviorProjection {
    override val id = ENTRY_MISSING_CHILD_GAP_BEHAVIOR_ID
}

internal object EntryChildListBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.child-list.behavior")
}

internal object EntryChildListFeatureContributor : FeatureGraphContributor {
    override val owner = ENTRY_CHILD_LIST_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_CHILD_LIST_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_CHILD_LIST_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Provided(EntryChildListCapability.definition),
                        behaviorProjections = listOf(
                            EntryChildListOrderBehavior,
                            EntryChildListFirstChildBehavior,
                            EntryChildListDisplayBehavior,
                        ),
                        behavioralContracts = listOf(EntryChildListBehaviorContract),
                    ),
                    FeatureIntegration(
                        id = ENTRY_CHILD_PROGRESS_INTEGRATION_ID,
                        prerequisites = allOf(
                            CapabilityExpression.Provided(EntryChildListCapability.definition),
                            CapabilityExpression.Provided(EntryChildProgressCapability.definition),
                        ),
                        behaviorProjections = listOf(EntryChildProgressBehavior),
                        projectionRequirements = listOf(ENTRY_CHILD_PROGRESS_REFERENCE.requirement),
                        projections = listOf(ENTRY_CHILD_PROGRESS_REFERENCE.projection),
                    ),
                    FeatureIntegration(
                        id = ENTRY_MISSING_CHILD_GAP_INTEGRATION_ID,
                        prerequisites = allOf(
                            CapabilityExpression.Provided(EntryChildListCapability.definition),
                            CapabilityExpression.Provided(EntryMissingChildGapCapability.definition),
                        ),
                        behaviorProjections = listOf(EntryMissingChildGapBehavior),
                        projectionRequirements = listOf(ENTRY_MISSING_CHILD_GAP_REFERENCE.requirement),
                        projections = listOf(ENTRY_MISSING_CHILD_GAP_REFERENCE.projection),
                    ),
                ),
            ),
        )
    }
}

internal class DefaultEntryChildListFeature(
    evaluation: FeatureGraphEvaluation,
    private val childList: EntryChildListInteraction,
    private val childProgress: EntryChildProgressInteraction,
    private val missingChildGap: EntryMissingChildGapInteraction,
) : EntryChildListFeature {
    private val orderTypes = evaluation.applicableProviderTypes<EntryChildListProcessor>(
        feature = ENTRY_CHILD_LIST_FEATURE_ID,
        integration = ENTRY_CHILD_LIST_INTEGRATION_ID,
        behaviorProjection = ENTRY_CHILD_LIST_ORDER_BEHAVIOR_ID,
    )
    private val displayTypes = evaluation.applicableProviderTypes<EntryChildListProcessor>(
        feature = ENTRY_CHILD_LIST_FEATURE_ID,
        integration = ENTRY_CHILD_LIST_INTEGRATION_ID,
        behaviorProjection = ENTRY_CHILD_LIST_DISPLAY_BEHAVIOR_ID,
    )
    private val firstChildTypes = evaluation.applicableProviderTypes<EntryChildListProcessor>(
        feature = ENTRY_CHILD_LIST_FEATURE_ID,
        integration = ENTRY_CHILD_LIST_INTEGRATION_ID,
        behaviorProjection = ENTRY_CHILD_LIST_FIRST_CHILD_BEHAVIOR_ID,
    )
    private val progressTypes = evaluation.applicableProviderTypes<EntryChildProgressProcessor>(
        feature = ENTRY_CHILD_LIST_FEATURE_ID,
        integration = ENTRY_CHILD_PROGRESS_INTEGRATION_ID,
        behaviorProjection = ENTRY_CHILD_PROGRESS_BEHAVIOR_ID,
    )
    private val missingGapTypes = evaluation.applicableProviderTypes<EntryMissingChildGapProcessor>(
        feature = ENTRY_CHILD_LIST_FEATURE_ID,
        integration = ENTRY_MISSING_CHILD_GAP_INTEGRATION_ID,
        behaviorProjection = ENTRY_MISSING_CHILD_GAP_BEHAVIOR_ID,
    )

    init {
        check(setOf(orderTypes, firstChildTypes, displayTypes).size == 1) {
            "Child-list order, first-child, and display behaviors selected different provider sets"
        }
    }

    override fun isApplicable(type: EntryType): Boolean = type in orderTypes

    override fun readingOrder(
        entry: Entry,
        chapters: List<EntryChapter>,
        memberIds: List<Long>,
    ): EntryChildOrderResult {
        if (entry.type !in orderTypes) return EntryChildOrderResult.Inapplicable(entry.type)
        return EntryChildOrderResult.Available(childList.sortedForReading(entry, chapters, memberIds))
    }

    override fun displayOrder(
        entry: Entry,
        chapters: List<EntryChapter>,
        memberIds: List<Long>,
    ): EntryChildOrderResult {
        if (entry.type !in orderTypes) return EntryChildOrderResult.Inapplicable(entry.type)
        return EntryChildOrderResult.Available(childList.sortedForDisplay(entry, chapters, memberIds))
    }

    override fun firstReadingChild(
        entry: Entry,
        chapters: List<EntryChapter>,
        memberIds: List<Long>,
    ): EntryFirstChildResult {
        if (entry.type !in firstChildTypes) return EntryFirstChildResult.Inapplicable(entry.type)
        return EntryFirstChildResult.Available(
            childList.sortedForReading(entry, chapters, memberIds).firstOrNull(),
        )
    }

    override fun displayList(request: EntryChildListRequest): EntryChildListResult {
        if (request.entry.type !in displayTypes) return EntryChildListResult.Inapplicable(request.entry.type)
        val display = if (request.entry.type in missingGapTypes) {
            missingChildGap.buildDisplayList(request)
        } else {
            buildDefaultDisplayList(request)
        }
        return EntryChildListResult.Available(display)
    }

    override fun progressLabels(request: EntryChildProgressRequest): EntryChildProgressResult {
        if (request.entry.type !in progressTypes) return EntryChildProgressResult.Inapplicable(request.entry.type)
        return EntryChildProgressResult.Available(childProgress.progressLabels(request))
    }

    private fun buildDefaultDisplayList(request: EntryChildListRequest): EntryChildListDisplay {
        val sortedChildren = childList.sortedForDisplay(request.entry, request.chapters, request.memberIds)
        val rows: List<EntryChildListRow> = if (request.memberIds.size <= 1) {
            sortedChildren.map(EntryChildListRow::Child)
        } else {
            buildList<EntryChildListRow> {
                request.memberIds.forEach { memberId ->
                    val memberChildren = sortedChildren.filter { it.entryId == memberId }
                    if (memberChildren.isEmpty()) return@forEach
                    add(
                        EntryChildListRow.MemberHeader(
                            entryId = memberId,
                            title = request.memberTitleById[memberId].orEmpty().ifBlank { request.fallbackTitle },
                        ),
                    )
                    addAll(memberChildren.map(EntryChildListRow::Child))
                }
            }
        }
        return EntryChildListDisplay(rows = rows, aggregateMissingCount = 0)
    }
}
