package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.ConfigurableSource
import eu.kanade.tachiyomi.source.entry.EntryImageSource
import eu.kanade.tachiyomi.source.entry.SourceHomePage
import mihon.entry.interactions.documentation.EntryContentTypeReferenceSection
import mihon.entry.interactions.documentation.EntryContentTypeReferenceSelection
import mihon.entry.interactions.documentation.EntryContentTypeReferenceStatus
import mihon.entry.interactions.documentation.entryContentTypeReferenceContribution
import mihon.entry.interactions.documentation.source.ENTRY_SOURCE_HOME_CONTEXT_OWNER
import mihon.entry.interactions.documentation.source.ENTRY_SOURCE_SETTINGS_CONTEXT_OWNER
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

internal val TRACKER_SOURCE_ADAPTER_FEATURE_ID = FeatureId("entry.tracker-source-adapter")
private val TRACKER_SOURCE_ADAPTER_OWNER = ContributionOwner("entry-tracker-source-adapter")
private val TRACKER_SOURCE_ADAPTER_REFERENCE = entryContentTypeReferenceContribution(
    id = "tracker-source-adapter",
    owner = TRACKER_SOURCE_ADAPTER_OWNER,
    section = EntryContentTypeReferenceSection.DISCOVERY_AND_INTEGRATIONS,
    label = "Use tracking-service sources in discovery",
    order = 1300,
    selection = EntryContentTypeReferenceSelection.CONDITIONAL_RELATIONSHIP,
    project = { EntryContentTypeReferenceStatus.SOURCE_DEPENDENT },
)
internal val TRACKER_SOURCE_ADAPTER_INTEGRATION_ID = FeatureIntegrationId("entry.tracker-source-adapter.connection")

internal object EntryTrackerSourceAdapterBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.tracker-source-adapter.behavior")
}

internal val TRACKER_SOURCE_SETTINGS_CONTEXT = entrySourceContextInputDefinition<Boolean>(
    id = ContextInputId("entry.tracker-source-adapter.settings"),
    owner = ENTRY_SOURCE_SETTINGS_CONTEXT_OWNER,
    contracts = setOf(ConfigurableSource::class),
)
internal val TRACKER_SOURCE_HOME_CONTEXT = entrySourceContextInputDefinition<Boolean>(
    id = ContextInputId("entry.tracker-source-adapter.home"),
    owner = ENTRY_SOURCE_HOME_CONTEXT_OWNER,
    contracts = setOf(SourceHomePage::class),
)
internal val TRACKER_SOURCE_IMAGE_CLIENT_CONTEXT = entrySourceContextInputDefinition<Boolean>(
    id = ContextInputId("entry.tracker-source-adapter.image-client"),
    contracts = setOf(EntryImageSource::class),
)
private val TRACKER_SOURCE_SETTINGS_UNAVAILABLE = FeatureContextBlocker(
    FeatureArtifactId("entry.tracker-source-adapter.settings-unavailable"),
    listOf(TRACKER_SOURCE_SETTINGS_CONTEXT),
)
private val TRACKER_SOURCE_HOME_UNAVAILABLE = FeatureContextBlocker(
    FeatureArtifactId("entry.tracker-source-adapter.home-unavailable"),
    listOf(TRACKER_SOURCE_HOME_CONTEXT),
)
private val TRACKER_SOURCE_IMAGE_CLIENT_UNAVAILABLE = FeatureContextBlocker(
    FeatureArtifactId("entry.tracker-source-adapter.image-client-unavailable"),
    listOf(TRACKER_SOURCE_IMAGE_CLIENT_CONTEXT),
)
private object TrackerSourceAdapterBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.tracker-source-adapter.connection")
}

internal object EntryTrackerSourceAdapterFeatureContributor : FeatureGraphContributor {
    override val owner = TRACKER_SOURCE_ADAPTER_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = TRACKER_SOURCE_ADAPTER_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = TRACKER_SOURCE_ADAPTER_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Always,
                        contextInputs = listOf(
                            TRACKER_SOURCE_SETTINGS_CONTEXT,
                            TRACKER_SOURCE_HOME_CONTEXT,
                            TRACKER_SOURCE_IMAGE_CLIENT_CONTEXT,
                        ),
                        contextRule = featureContextRule(owner) { evidence ->
                            val blockers = buildList {
                                if (!evidence.value(TRACKER_SOURCE_SETTINGS_CONTEXT)) {
                                    add(TRACKER_SOURCE_SETTINGS_UNAVAILABLE)
                                }
                                if (!evidence.value(TRACKER_SOURCE_HOME_CONTEXT)) {
                                    add(TRACKER_SOURCE_HOME_UNAVAILABLE)
                                }
                                if (!evidence.value(TRACKER_SOURCE_IMAGE_CLIENT_CONTEXT)) {
                                    add(TRACKER_SOURCE_IMAGE_CLIENT_UNAVAILABLE)
                                }
                            }
                            if (blockers.isEmpty()) {
                                FeatureContextDecision.Applicable
                            } else {
                                FeatureContextDecision.Blocked(blockers)
                            }
                        },
                        contextBlockers = listOf(
                            TRACKER_SOURCE_SETTINGS_UNAVAILABLE,
                            TRACKER_SOURCE_HOME_UNAVAILABLE,
                            TRACKER_SOURCE_IMAGE_CLIENT_UNAVAILABLE,
                        ),
                        behaviorProjections = listOf(TrackerSourceAdapterBehavior),
                        behavioralContracts = listOf(EntryTrackerSourceAdapterBehaviorContract),
                        projectionRequirements = listOf(TRACKER_SOURCE_ADAPTER_REFERENCE.requirement),
                        projections = listOf(TRACKER_SOURCE_ADAPTER_REFERENCE.projection),
                    ),
                ),
            ),
        )
    }
}

internal class DefaultEntryTrackerSourceAdapterFeature(
    private val evaluation: FeatureGraphEvaluation,
    private val sourceManager: SourceManager,
    private val settings: EntrySourceSettingsFeature,
    private val home: EntrySourceHomeFeature,
) : EntryTrackerSourceAdapterFeature {

    override fun resolve(sourceId: Long): EntryTrackerSourceAdapterResolution {
        val settingsResult = settings.resolve(sourceId)
        val homeResult = home.resolve(sourceId)
        val imageSource = sourceManager.get(sourceId) as? EntryImageSource
        val applicable = settingsResult is EntrySourceSettingsResolution.Available &&
            homeResult is EntrySourceHomeResolution.Available && imageSource != null
        evaluation.requireSourceContextState(
            feature = TRACKER_SOURCE_ADAPTER_FEATURE_ID,
            integration = TRACKER_SOURCE_ADAPTER_INTEGRATION_ID,
            behaviorProjection = TrackerSourceAdapterBehavior.id,
            evidence = listOf(
                contextEvidence(
                    TRACKER_SOURCE_SETTINGS_CONTEXT,
                    settingsResult is EntrySourceSettingsResolution.Available,
                ),
                contextEvidence(
                    TRACKER_SOURCE_HOME_CONTEXT,
                    homeResult is EntrySourceHomeResolution.Available,
                ),
                contextEvidence(TRACKER_SOURCE_IMAGE_CLIENT_CONTEXT, imageSource != null),
            ),
            applicable = applicable,
        )

        val failure = when {
            settingsResult is EntrySourceSettingsResolution.Failed -> settingsResult.cause
            homeResult is EntrySourceHomeResolution.Failed -> homeResult.cause
            else -> null
        }
        if (failure != null) return EntryTrackerSourceAdapterResolution.Failed(sourceId, failure)
        if (!applicable) {
            return EntryTrackerSourceAdapterResolution.Unavailable(
                sourceId = sourceId,
                reasons = buildSet {
                    if (settingsResult !is EntrySourceSettingsResolution.Available) {
                        add(EntryTrackerSourceAdapterUnavailableReason.SETTINGS)
                    }
                    if (homeResult !is EntrySourceHomeResolution.Available) {
                        add(EntryTrackerSourceAdapterUnavailableReason.HOME)
                    }
                    if (imageSource == null) add(EntryTrackerSourceAdapterUnavailableReason.IMAGE_CLIENT)
                },
            )
        }

        return EntryTrackerSourceAdapterResolution.Available(
            sourceId = sourceId,
            preferences = (settingsResult as EntrySourceSettingsResolution.Available).preferences,
            homeUrl = (homeResult as EntrySourceHomeResolution.Available).url,
            imageClient = checkNotNull(imageSource).client,
        )
    }
}
