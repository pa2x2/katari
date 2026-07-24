package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.documentation.EntryContentTypeReferenceSection
import mihon.entry.interactions.documentation.entryContentTypeReferenceContribution
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContentTypeId
import mihon.feature.graph.ContextInputId
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContextBlocker
import mihon.feature.graph.FeatureContextDecision
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.contextEvidence
import mihon.feature.graph.contextInputDefinition
import mihon.feature.graph.featureContextRule

internal val ENTRY_UPDATE_ELIGIBILITY_FEATURE_ID = FeatureId("entry.update-eligibility")
private val FEATURE_OWNER = ContributionOwner("entry-update-eligibility")
private val ENTRY_UPDATE_ELIGIBILITY_REFERENCE = entryContentTypeReferenceContribution(
    id = "smart-library-update",
    owner = FEATURE_OWNER,
    section = EntryContentTypeReferenceSection.ENTRY_INTERACTIONS,
    label = "Apply smart library-update restrictions",
    order = 400,
)
internal val ENTRY_UPDATE_ELIGIBILITY_POLICY_INTEGRATION =
    FeatureIntegrationId("entry.update-eligibility.shared-policy")
internal val ENTRY_UPDATE_ELIGIBILITY_DECISION_INTEGRATION =
    FeatureIntegrationId("entry.update-eligibility.decision")

private enum class EntryUpdateEligibilityPolicyBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    TYPE_PARTICIPATION(FeatureArtifactId("entry.update-eligibility.type-participation")),
    POLICY_AVAILABILITY(FeatureArtifactId("entry.update-eligibility.policy-availability")),
    SMART_UPDATE_SETTINGS(FeatureArtifactId("entry.update-eligibility.smart-update-settings")),
}

private enum class EntryUpdateEligibilityDecisionBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    POLICY_DECISION(FeatureArtifactId("entry.update-eligibility.policy")),
    LIBRARY_UPDATE(FeatureArtifactId("entry.update-eligibility.library-update")),
    STATS(FeatureArtifactId("entry.update-eligibility.stats")),
}

internal object EntryUpdateEligibilityBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.update-eligibility.behavior")
}

internal object EntryUpdateEligibilityDecisionBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.update-eligibility.decision-behavior")
}

internal data class EntryUpdateEligibilityPolicy(
    val skipCompleted: Boolean,
    val skipWhenUnconsumed: Boolean,
    val skipWhenNotStarted: Boolean,
    val skipOutsideReleasePeriod: Boolean,
)

internal data class EntryUpdateEligibilityContext(
    val oneShotAlreadyFetched: Boolean,
    val completed: Boolean,
    val hasUnconsumed: Boolean,
    val notStartedWithChildren: Boolean,
    val outsideReleasePeriod: Boolean,
)

internal val ENTRY_UPDATE_ELIGIBILITY_POLICY_CONTEXT = contextInputDefinition<EntryUpdateEligibilityPolicy>(
    ContextInputId("entry.update-eligibility.configuration"),
    ContributionOwner("entry-update-configuration"),
)
internal val ENTRY_UPDATE_ELIGIBILITY_ONE_SHOT_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.update-eligibility.one-shot-already-fetched"),
    ContributionOwner("entry-state"),
)
internal val ENTRY_UPDATE_ELIGIBILITY_COMPLETED_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.update-eligibility.completed"),
    ContributionOwner("entry-state"),
)
internal val ENTRY_UPDATE_ELIGIBILITY_UNCONSUMED_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.update-eligibility.has-unconsumed"),
    ContributionOwner("entry-state"),
)
internal val ENTRY_UPDATE_ELIGIBILITY_NOT_STARTED_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.update-eligibility.not-started-with-children"),
    ContributionOwner("entry-state"),
)
internal val ENTRY_UPDATE_ELIGIBILITY_RELEASE_PERIOD_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.update-eligibility.outside-release-period"),
    ContributionOwner("entry-update-window"),
)

private val NOT_ALWAYS_UPDATE_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.update-eligibility.not-always-update"),
    listOf(ENTRY_UPDATE_ELIGIBILITY_ONE_SHOT_CONTEXT),
)
private val COMPLETED_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.update-eligibility.skip-completed"),
    listOf(ENTRY_UPDATE_ELIGIBILITY_POLICY_CONTEXT, ENTRY_UPDATE_ELIGIBILITY_COMPLETED_CONTEXT),
)
private val NOT_CAUGHT_UP_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.update-eligibility.not-caught-up"),
    listOf(ENTRY_UPDATE_ELIGIBILITY_POLICY_CONTEXT, ENTRY_UPDATE_ELIGIBILITY_UNCONSUMED_CONTEXT),
)
private val NOT_STARTED_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.update-eligibility.not-started"),
    listOf(ENTRY_UPDATE_ELIGIBILITY_POLICY_CONTEXT, ENTRY_UPDATE_ELIGIBILITY_NOT_STARTED_CONTEXT),
)
private val OUTSIDE_RELEASE_PERIOD_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.update-eligibility.outside-release-period-blocker"),
    listOf(ENTRY_UPDATE_ELIGIBILITY_POLICY_CONTEXT, ENTRY_UPDATE_ELIGIBILITY_RELEASE_PERIOD_CONTEXT),
)

internal object EntryUpdateEligibilityFeatureContributor : FeatureGraphContributor {
    override val owner = FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_UPDATE_ELIGIBILITY_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_UPDATE_ELIGIBILITY_POLICY_INTEGRATION,
                        prerequisites = CapabilityExpression.Always,
                        behaviorProjections = EntryUpdateEligibilityPolicyBehavior.entries,
                        behavioralContracts = listOf(EntryUpdateEligibilityBehaviorContract),
                        projectionRequirements = listOf(ENTRY_UPDATE_ELIGIBILITY_REFERENCE.requirement),
                        projections = listOf(ENTRY_UPDATE_ELIGIBILITY_REFERENCE.projection),
                    ),
                    FeatureIntegration(
                        id = ENTRY_UPDATE_ELIGIBILITY_DECISION_INTEGRATION,
                        prerequisites = CapabilityExpression.Always,
                        contextInputs = listOf(
                            ENTRY_UPDATE_ELIGIBILITY_POLICY_CONTEXT,
                            ENTRY_UPDATE_ELIGIBILITY_ONE_SHOT_CONTEXT,
                            ENTRY_UPDATE_ELIGIBILITY_COMPLETED_CONTEXT,
                            ENTRY_UPDATE_ELIGIBILITY_UNCONSUMED_CONTEXT,
                            ENTRY_UPDATE_ELIGIBILITY_NOT_STARTED_CONTEXT,
                            ENTRY_UPDATE_ELIGIBILITY_RELEASE_PERIOD_CONTEXT,
                        ),
                        contextRule = featureContextRule(owner) { evidence ->
                            val policy = evidence.value(ENTRY_UPDATE_ELIGIBILITY_POLICY_CONTEXT)
                            when {
                                evidence.value(ENTRY_UPDATE_ELIGIBILITY_ONE_SHOT_CONTEXT) ->
                                    FeatureContextDecision.Blocked(listOf(NOT_ALWAYS_UPDATE_BLOCKER))
                                policy.skipCompleted && evidence.value(ENTRY_UPDATE_ELIGIBILITY_COMPLETED_CONTEXT) ->
                                    FeatureContextDecision.Blocked(listOf(COMPLETED_BLOCKER))
                                policy.skipWhenUnconsumed && evidence.value(
                                    ENTRY_UPDATE_ELIGIBILITY_UNCONSUMED_CONTEXT,
                                ) ->
                                    FeatureContextDecision.Blocked(listOf(NOT_CAUGHT_UP_BLOCKER))
                                policy.skipWhenNotStarted &&
                                    evidence.value(ENTRY_UPDATE_ELIGIBILITY_NOT_STARTED_CONTEXT) ->
                                    FeatureContextDecision.Blocked(listOf(NOT_STARTED_BLOCKER))
                                policy.skipOutsideReleasePeriod &&
                                    evidence.value(ENTRY_UPDATE_ELIGIBILITY_RELEASE_PERIOD_CONTEXT) ->
                                    FeatureContextDecision.Blocked(listOf(OUTSIDE_RELEASE_PERIOD_BLOCKER))
                                else -> FeatureContextDecision.Applicable
                            }
                        },
                        contextBlockers = listOf(
                            NOT_ALWAYS_UPDATE_BLOCKER,
                            COMPLETED_BLOCKER,
                            NOT_CAUGHT_UP_BLOCKER,
                            NOT_STARTED_BLOCKER,
                            OUTSIDE_RELEASE_PERIOD_BLOCKER,
                        ),
                        behaviorProjections = EntryUpdateEligibilityDecisionBehavior.entries,
                        behavioralContracts = listOf(EntryUpdateEligibilityDecisionBehaviorContract),
                    ),
                ),
            ),
        )
    }
}

internal fun FeatureGraphEvaluation.updateEligibilityContentTypes(): Set<ContentTypeId> =
    behaviorProjections
        .asSequence()
        .filter { applicability ->
            applicability.subject.feature == ENTRY_UPDATE_ELIGIBILITY_FEATURE_ID &&
                applicability.subject.integration == ENTRY_UPDATE_ELIGIBILITY_POLICY_INTEGRATION &&
                applicability.projection.id == EntryUpdateEligibilityPolicyBehavior.TYPE_PARTICIPATION.id
        }
        .mapTo(mutableSetOf()) { it.subject.contentType }

internal fun FeatureGraphEvaluation.requireUpdateEligibilityContext(
    type: EntryType,
    policy: EntryUpdateEligibilityPolicy,
    context: EntryUpdateEligibilityContext,
    applicable: Boolean,
) {
    requireEntryContextState(
        type = type,
        feature = ENTRY_UPDATE_ELIGIBILITY_FEATURE_ID,
        integration = ENTRY_UPDATE_ELIGIBILITY_DECISION_INTEGRATION,
        behaviorProjections = EntryUpdateEligibilityDecisionBehavior.entries.map(
            EntryUpdateEligibilityDecisionBehavior::id,
        ),
        evidence = listOf(
            contextEvidence(ENTRY_UPDATE_ELIGIBILITY_POLICY_CONTEXT, policy),
            contextEvidence(ENTRY_UPDATE_ELIGIBILITY_ONE_SHOT_CONTEXT, context.oneShotAlreadyFetched),
            contextEvidence(ENTRY_UPDATE_ELIGIBILITY_COMPLETED_CONTEXT, context.completed),
            contextEvidence(ENTRY_UPDATE_ELIGIBILITY_UNCONSUMED_CONTEXT, context.hasUnconsumed),
            contextEvidence(ENTRY_UPDATE_ELIGIBILITY_NOT_STARTED_CONTEXT, context.notStartedWithChildren),
            contextEvidence(ENTRY_UPDATE_ELIGIBILITY_RELEASE_PERIOD_CONTEXT, context.outsideReleasePeriod),
        ),
        applicable = applicable,
    )
}
