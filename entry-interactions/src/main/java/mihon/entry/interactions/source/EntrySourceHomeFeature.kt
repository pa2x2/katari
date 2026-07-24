package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.SourceHomePage
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

internal val SOURCE_HOME_FEATURE_ID = FeatureId("entry.source-home")
private val SOURCE_HOME_OWNER = ContributionOwner("entry-source-home")
private val SOURCE_HOME_REFERENCE = entryContentTypeReferenceContribution(
    id = "source-home",
    owner = SOURCE_HOME_OWNER,
    section = EntryContentTypeReferenceSection.DISCOVERY_AND_INTEGRATIONS,
    label = "Open source home pages",
    order = 800,
    selection = EntryContentTypeReferenceSelection.CONDITIONAL_RELATIONSHIP,
    project = { EntryContentTypeReferenceStatus.SOURCE_DEPENDENT },
)
internal val SOURCE_HOME_INTEGRATION_ID = FeatureIntegrationId("entry.source-home.navigation")

internal object EntrySourceHomeBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.source-home.behavior")
}

internal enum class SourceHomeUrlState {
    AVAILABLE,
    ABSENT,
    FAILED,
}

internal data class SourceHomeContext(
    val installed: Boolean,
    val supported: Boolean,
    val urlState: SourceHomeUrlState,
)

internal val SOURCE_HOME_CONTEXT = entrySourceContextInputDefinition<SourceHomeContext>(
    id = ContextInputId("entry.source-home.context"),
    contracts = setOf(SourceHomePage::class),
)
private val SOURCE_HOME_MISSING = FeatureContextBlocker(
    FeatureArtifactId("entry.source-home.source-missing"),
    listOf(SOURCE_HOME_CONTEXT),
)
private val SOURCE_HOME_UNSUPPORTED = FeatureContextBlocker(
    FeatureArtifactId("entry.source-home.unsupported"),
    listOf(SOURCE_HOME_CONTEXT),
)
private val SOURCE_HOME_NO_URL = FeatureContextBlocker(
    FeatureArtifactId("entry.source-home.no-url"),
    listOf(SOURCE_HOME_CONTEXT),
)
private enum class SourceHomeBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    NAVIGATION(FeatureArtifactId("entry.source-home.navigation")),
    SEARCH_MATCHING(FeatureArtifactId("entry.source-home.search-matching")),
    COOKIE_MAINTENANCE(FeatureArtifactId("entry.source-home.cookie-maintenance")),
    TRACKER_ADAPTER(FeatureArtifactId("entry.source-home.tracker-adapter")),
}

internal object EntrySourceHomeFeatureContributor : FeatureGraphContributor {
    override val owner = SOURCE_HOME_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = SOURCE_HOME_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = SOURCE_HOME_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Always,
                        contextInputs = listOf(SOURCE_HOME_CONTEXT),
                        contextRule = featureContextRule(owner) { evidence ->
                            val context = evidence.value(SOURCE_HOME_CONTEXT)
                            when {
                                !context.installed -> FeatureContextDecision.Blocked(listOf(SOURCE_HOME_MISSING))
                                !context.supported -> FeatureContextDecision.Blocked(listOf(SOURCE_HOME_UNSUPPORTED))
                                context.urlState == SourceHomeUrlState.ABSENT ->
                                    FeatureContextDecision.Blocked(listOf(SOURCE_HOME_NO_URL))
                                else -> FeatureContextDecision.Applicable
                            }
                        },
                        contextBlockers = listOf(SOURCE_HOME_MISSING, SOURCE_HOME_UNSUPPORTED, SOURCE_HOME_NO_URL),
                        behaviorProjections = SourceHomeBehavior.entries,
                        behavioralContracts = listOf(EntrySourceHomeBehaviorContract),
                        projectionRequirements = listOf(SOURCE_HOME_REFERENCE.requirement),
                        projections = listOf(SOURCE_HOME_REFERENCE.projection),
                    ),
                ),
            ),
        )
    }
}

internal class DefaultEntrySourceHomeFeature(
    private val evaluation: FeatureGraphEvaluation,
    private val sourceManager: SourceManager,
) : EntrySourceHomeFeature {

    override fun resolve(sourceId: Long): EntrySourceHomeResolution {
        val source = sourceManager.get(sourceId)
            ?: return result(sourceId, installed = false, provider = null, url = null)
        val provider = source as? SourceHomePage
            ?: return result(sourceId, installed = true, provider = null, url = null)
        val url = try {
            provider.getHomeUrl()
        } catch (error: Throwable) {
            requireState(
                installed = true,
                supported = true,
                urlState = SourceHomeUrlState.FAILED,
                applicable = true,
            )
            return EntrySourceHomeResolution.Failed(sourceId, error)
        }
        return result(sourceId, installed = true, provider = provider, url = url)
    }

    private fun result(
        sourceId: Long,
        installed: Boolean,
        provider: SourceHomePage?,
        url: String?,
    ): EntrySourceHomeResolution {
        val hasUrl = !url.isNullOrBlank()
        requireState(
            installed = installed,
            supported = provider != null,
            urlState = if (hasUrl) SourceHomeUrlState.AVAILABLE else SourceHomeUrlState.ABSENT,
            applicable = installed && provider != null && hasUrl,
        )
        return when {
            !installed -> EntrySourceHomeResolution.Missing(sourceId)
            provider == null -> EntrySourceHomeResolution.Unsupported(sourceId)
            !hasUrl -> EntrySourceHomeResolution.NoUrl(sourceId)
            else -> EntrySourceHomeResolution.Available(sourceId, provider.name, url)
        }
    }

    private fun requireState(
        installed: Boolean,
        supported: Boolean,
        urlState: SourceHomeUrlState,
        applicable: Boolean,
    ) {
        SourceHomeBehavior.entries.forEach { behavior ->
            evaluation.requireSourceContextState(
                feature = SOURCE_HOME_FEATURE_ID,
                integration = SOURCE_HOME_INTEGRATION_ID,
                behaviorProjection = behavior.id,
                evidence = listOf(
                    contextEvidence(SOURCE_HOME_CONTEXT, SourceHomeContext(installed, supported, urlState)),
                ),
                applicable = applicable,
            )
        }
    }
}
