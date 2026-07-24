package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.ConfigurableSource
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

internal val SOURCE_SETTINGS_FEATURE_ID = FeatureId("entry.source-settings")
private val SOURCE_SETTINGS_OWNER = ContributionOwner("entry-source-settings")
private val SOURCE_SETTINGS_REFERENCE = entryContentTypeReferenceContribution(
    id = "source-settings",
    owner = SOURCE_SETTINGS_OWNER,
    section = EntryContentTypeReferenceSection.DISCOVERY_AND_INTEGRATIONS,
    label = "Configure source-specific settings",
    order = 1000,
    selection = EntryContentTypeReferenceSelection.CONDITIONAL_RELATIONSHIP,
    project = { EntryContentTypeReferenceStatus.SOURCE_DEPENDENT },
)
internal val SOURCE_SETTINGS_INTEGRATION_ID = FeatureIntegrationId("entry.source-settings.access")

internal object EntrySourceSettingsBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.source-settings.behavior")
}

internal data class SourceSettingsContext(val installed: Boolean, val configurable: Boolean)

internal val SOURCE_SETTINGS_CONTEXT = entrySourceContextInputDefinition<SourceSettingsContext>(
    id = ContextInputId("entry.source-settings.context"),
    contracts = setOf(ConfigurableSource::class),
)
private val SOURCE_SETTINGS_MISSING = FeatureContextBlocker(
    FeatureArtifactId("entry.source-settings.source-missing"),
    listOf(SOURCE_SETTINGS_CONTEXT),
)
private val SOURCE_SETTINGS_UNSUPPORTED = FeatureContextBlocker(
    FeatureArtifactId("entry.source-settings.unsupported"),
    listOf(SOURCE_SETTINGS_CONTEXT),
)
private enum class SourceSettingsBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    AVAILABILITY(FeatureArtifactId("entry.source-settings.availability")),
    PREFERENCE_SCREEN(FeatureArtifactId("entry.source-settings.preference-screen")),
    BACKUP(FeatureArtifactId("entry.source-settings.backup")),
    TRACKER_ADAPTER(FeatureArtifactId("entry.source-settings.tracker-adapter")),
}

internal object EntrySourceSettingsFeatureContributor : FeatureGraphContributor {
    override val owner = SOURCE_SETTINGS_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = SOURCE_SETTINGS_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = SOURCE_SETTINGS_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Always,
                        contextInputs = listOf(SOURCE_SETTINGS_CONTEXT),
                        contextRule = featureContextRule(owner) { evidence ->
                            val context = evidence.value(SOURCE_SETTINGS_CONTEXT)
                            when {
                                !context.installed -> FeatureContextDecision.Blocked(listOf(SOURCE_SETTINGS_MISSING))
                                !context.configurable ->
                                    FeatureContextDecision.Blocked(listOf(SOURCE_SETTINGS_UNSUPPORTED))
                                else -> FeatureContextDecision.Applicable
                            }
                        },
                        contextBlockers = listOf(SOURCE_SETTINGS_MISSING, SOURCE_SETTINGS_UNSUPPORTED),
                        behaviorProjections = SourceSettingsBehavior.entries,
                        behavioralContracts = listOf(EntrySourceSettingsBehaviorContract),
                        projectionRequirements = listOf(SOURCE_SETTINGS_REFERENCE.requirement),
                        projections = listOf(SOURCE_SETTINGS_REFERENCE.projection),
                    ),
                ),
            ),
        )
    }
}

internal class DefaultEntrySourceSettingsFeature(
    private val evaluation: FeatureGraphEvaluation,
    private val sourceManager: SourceManager,
) : EntrySourceSettingsFeature {

    override fun resolve(sourceId: Long): EntrySourceSettingsResolution {
        val source = sourceManager.get(sourceId)
        val configurable = source as? ConfigurableSource
        requireState(installed = source != null, configurable = configurable != null)
        if (source == null) return EntrySourceSettingsResolution.Missing(sourceId)
        if (configurable == null) return EntrySourceSettingsResolution.Unsupported(sourceId)

        return runCatching {
            EntrySourceSettingsResolution.Available(
                sourceId = sourceId,
                preferences = configurable.getSourcePreferences(),
                populateScreen = configurable::setupPreferenceScreen,
            )
        }.getOrElse { EntrySourceSettingsResolution.Failed(sourceId, it) }
    }

    override fun supportedSourceIds(): List<Long> {
        return sourceManager.getAll()
            .filterIsInstance<ConfigurableSource>()
            .onEach { requireState(installed = true, configurable = true) }
            .map { it.id }
            .sorted()
    }

    private fun requireState(installed: Boolean, configurable: Boolean) {
        SourceSettingsBehavior.entries.forEach { behavior ->
            evaluation.requireSourceContextState(
                feature = SOURCE_SETTINGS_FEATURE_ID,
                integration = SOURCE_SETTINGS_INTEGRATION_ID,
                behaviorProjection = behavior.id,
                evidence = listOf(
                    contextEvidence(SOURCE_SETTINGS_CONTEXT, SourceSettingsContext(installed, configurable)),
                ),
                applicable = installed && configurable,
            )
        }
    }
}
