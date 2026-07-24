package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryImageSource
import mihon.entry.interactions.documentation.EntryContentTypeReferenceSection
import mihon.entry.interactions.documentation.EntryContentTypeReferenceSelection
import mihon.entry.interactions.documentation.EntryContentTypeReferenceStatus
import mihon.entry.interactions.documentation.entryContentTypeReferenceContribution
import mihon.entry.interactions.documentation.source.entrySourceContextInputDefinition
import mihon.feature.graph.CapabilityExpression
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
import tachiyomi.domain.source.service.SourceManager

internal val ENTRY_COVER_NETWORK_FEATURE_ID = FeatureId("entry.cover-network")
internal val ENTRY_COVER_NETWORK_OWNER = ContributionOwner("entry-cover-network")
private val ENTRY_COVER_NETWORK_REFERENCE = entryContentTypeReferenceContribution(
    id = "cover-network",
    owner = ENTRY_COVER_NETWORK_OWNER,
    section = EntryContentTypeReferenceSection.DISCOVERY_AND_INTEGRATIONS,
    label = "Load entry covers through source networking",
    order = 600,
    selection = EntryContentTypeReferenceSelection.CONDITIONAL_RELATIONSHIP,
    project = { EntryContentTypeReferenceStatus.SOURCE_DEPENDENT },
)
internal val ENTRY_COVER_NETWORK_INTEGRATION_ID = FeatureIntegrationId("entry.cover-network.source")

internal object EntryCoverNetworkBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.cover-network.behavior")
}

internal data class EntryCoverNetworkContext(val installed: Boolean, val supported: Boolean)

internal val ENTRY_COVER_NETWORK_CONTEXT = entrySourceContextInputDefinition<EntryCoverNetworkContext>(
    id = ContextInputId("entry.cover-network.source-context"),
    contracts = setOf(EntryImageSource::class),
)
private val ENTRY_COVER_NETWORK_SOURCE_MISSING = FeatureContextBlocker(
    FeatureArtifactId("entry.cover-network.source-missing"),
    listOf(ENTRY_COVER_NETWORK_CONTEXT),
)
private val ENTRY_COVER_NETWORK_UNSUPPORTED = FeatureContextBlocker(
    FeatureArtifactId("entry.cover-network.unsupported"),
    listOf(ENTRY_COVER_NETWORK_CONTEXT),
)
private enum class EntryCoverNetworkBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    CALL_FACTORY(FeatureArtifactId("entry.cover-network.call-factory")),
    REQUEST_HEADERS(FeatureArtifactId("entry.cover-network.request-headers")),
}

internal object EntryCoverNetworkFeatureContributor : FeatureGraphContributor {
    override val owner = ENTRY_COVER_NETWORK_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_COVER_NETWORK_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_COVER_NETWORK_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Always,
                        contextInputs = listOf(ENTRY_COVER_NETWORK_CONTEXT),
                        contextRule = featureContextRule(owner) { evidence ->
                            val context = evidence.value(ENTRY_COVER_NETWORK_CONTEXT)
                            when {
                                !context.installed ->
                                    FeatureContextDecision.Blocked(listOf(ENTRY_COVER_NETWORK_SOURCE_MISSING))
                                !context.supported ->
                                    FeatureContextDecision.Blocked(listOf(ENTRY_COVER_NETWORK_UNSUPPORTED))
                                else -> FeatureContextDecision.Applicable
                            }
                        },
                        contextBlockers = listOf(
                            ENTRY_COVER_NETWORK_SOURCE_MISSING,
                            ENTRY_COVER_NETWORK_UNSUPPORTED,
                        ),
                        behaviorProjections = EntryCoverNetworkBehavior.entries,
                        behavioralContracts = listOf(EntryCoverNetworkBehaviorContract),
                        projectionRequirements = listOf(ENTRY_COVER_NETWORK_REFERENCE.requirement),
                        projections = listOf(ENTRY_COVER_NETWORK_REFERENCE.projection),
                    ),
                ),
            ),
        )
    }
}

internal class DefaultEntryCoverNetworkFeature(
    private val evaluation: FeatureGraphEvaluation,
    private val sourceManager: SourceManager,
) : EntryCoverNetworkFeature {

    override fun resolve(sourceId: Long): EntryCoverNetworkResolution {
        val source = sourceManager.get(sourceId)
        val provider = source as? EntryImageSource
        requireState(installed = source != null, supported = provider != null)
        if (source == null) return EntryCoverNetworkResolution.Missing(sourceId)
        if (provider == null) return EntryCoverNetworkResolution.Unsupported(sourceId)

        return runCatching {
            EntryCoverNetworkResolution.Available(
                sourceId = sourceId,
                callFactory = provider.client,
                headers = provider.headers,
            )
        }.getOrElse { EntryCoverNetworkResolution.Failed(sourceId, it) }
    }

    private fun requireState(installed: Boolean, supported: Boolean) {
        EntryCoverNetworkBehavior.entries.forEach { behavior ->
            evaluation.requireSourceContextState(
                feature = ENTRY_COVER_NETWORK_FEATURE_ID,
                integration = ENTRY_COVER_NETWORK_INTEGRATION_ID,
                behaviorProjection = behavior.id,
                evidence = listOf(
                    contextEvidence(
                        ENTRY_COVER_NETWORK_CONTEXT,
                        EntryCoverNetworkContext(installed, supported),
                    ),
                ),
                applicable = installed && supported,
            )
        }
    }
}
