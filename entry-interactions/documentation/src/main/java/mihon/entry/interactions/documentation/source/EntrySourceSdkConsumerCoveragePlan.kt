package mihon.entry.interactions.documentation.source

import mihon.feature.graph.ContextInputId
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegrationId
import kotlin.reflect.KClass

data class EntrySourceSdkConsumerCoveragePlan(
    val consumers: List<EntrySourceSdkContextConsumer>,
    val exclusions: List<EntrySourceSdkContextExclusionRecord>,
    val issues: List<EntrySourceSdkConsumerCoverageIssue>,
) {
    val isComplete: Boolean
        get() = issues.isEmpty()
}

data class EntrySourceSdkContextConsumer(
    val contract: KClass<*>,
    val feature: FeatureId,
    val featureOwner: ContributionOwner,
    val integration: FeatureIntegrationId,
    val contextInput: ContextInputId,
)

data class EntrySourceSdkContextExclusionRecord(
    val feature: FeatureId,
    val integration: FeatureIntegrationId,
    val contextInput: ContextInputId,
    val owner: ContributionOwner,
    val reason: String,
)

data class EntrySourceSdkConsumerCoverageIssue(
    val responsibleOwner: ContributionOwner,
    val contextInput: ContextInputId,
    val details: String,
)
