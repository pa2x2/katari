package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.EntryItemOrientationProvider
import eu.kanade.tachiyomi.source.entry.SourceMetadata
import mihon.entry.interactions.documentation.EntryContentTypeReferenceContribution
import mihon.entry.interactions.documentation.EntryContentTypeReferenceSection
import mihon.entry.interactions.documentation.entryContentTypeReferenceContribution
import mihon.entry.interactions.documentation.source.ENTRY_SOURCE_CONTEXT_OWNER
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
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.contextInputDefinition
import mihon.feature.graph.featureContextRule
import tachiyomi.domain.source.model.EntrySourceDescription

internal val ENTRY_CATALOGUE_FEATURE_ID = FeatureId("entry.catalogue")
private val ENTRY_CATALOGUE_FEATURE_OWNER = ContributionOwner("entry-catalogue")
private val SOURCE_CONTRACT_OWNER = ENTRY_SOURCE_CONTEXT_OWNER

internal val SOURCE_DESCRIPTION_INTEGRATION_ID = FeatureIntegrationId("entry.catalogue.source-description")
internal val CATALOGUE_AVAILABILITY_INTEGRATION_ID = FeatureIntegrationId("entry.catalogue.availability")
internal val LATEST_AVAILABILITY_INTEGRATION_ID = FeatureIntegrationId("entry.catalogue.latest")
internal val LOCAL_SOURCE_REFERENCE_INTEGRATION_ID = FeatureIntegrationId("entry.catalogue.local-source-reference")
internal val LEGACY_SOURCE_REFERENCE_INTEGRATION_ID = FeatureIntegrationId("entry.catalogue.legacy-source-reference")

private val LOCAL_SOURCE_REFERENCE = entryContentTypeReferenceContribution(
    id = "local-source",
    owner = ENTRY_CATALOGUE_FEATURE_OWNER,
    section = EntryContentTypeReferenceSection.DISCOVERY_AND_INTEGRATIONS,
    label = "Import content through the bundled Local source",
    order = 300,
)
private val LEGACY_SOURCE_REFERENCE = entryContentTypeReferenceContribution(
    id = "legacy-extensions",
    owner = ENTRY_CATALOGUE_FEATURE_OWNER,
    section = EntryContentTypeReferenceSection.DISCOVERY_AND_INTEGRATIONS,
    label = "Use supported legacy Mihon extensions",
    order = 400,
)

internal object EntrySourceDescriptionBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.catalogue.source-description.behavior")
}

internal object EntryCatalogueAvailabilityBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.catalogue.availability.behavior")
}

internal object EntryLatestAvailabilityBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.catalogue.latest.behavior")
}

internal data class SourceDescriptionEvidence(
    val description: EntrySourceDescription,
)

internal val SOURCE_DESCRIPTION_CONTEXT = entrySourceContextInputDefinition<SourceDescriptionEvidence>(
    id = ContextInputId("entry.source.description"),
    owner = SOURCE_CONTRACT_OWNER,
    contracts = setOf(
        EntryCatalogueSource::class,
        SourceMetadata::class,
        EntryItemOrientationProvider::class,
    ),
    contractIntegrations = mapOf(
        SourceMetadata::class to setOf(SOURCE_DESCRIPTION_INTEGRATION_ID),
        EntryItemOrientationProvider::class to setOf(SOURCE_DESCRIPTION_INTEGRATION_ID),
    ),
)
internal val LOCAL_SOURCE_REGISTERED_SUPPORT_CONTEXT = contextInputDefinition<Boolean>(
    id = ContextInputId("entry.catalogue.local-source-registered-support"),
    owner = ContributionOwner("source-local"),
)
internal val LEGACY_SOURCE_REGISTERED_SUPPORT_CONTEXT = contextInputDefinition<Boolean>(
    id = ContextInputId("entry.catalogue.legacy-source-registered-support"),
    owner = ContributionOwner("source-compat"),
)

private val CATALOGUE_UNAVAILABLE_BLOCKER = FeatureContextBlocker(
    id = FeatureArtifactId("entry.catalogue.unavailable"),
    inputs = listOf(SOURCE_DESCRIPTION_CONTEXT),
)
private val LATEST_UNAVAILABLE_BLOCKER = FeatureContextBlocker(
    id = FeatureArtifactId("entry.catalogue.latest.unavailable"),
    inputs = listOf(SOURCE_DESCRIPTION_CONTEXT),
)
private val LOCAL_SOURCE_UNAVAILABLE_BLOCKER = FeatureContextBlocker(
    id = FeatureArtifactId("entry.catalogue.local-source-unavailable"),
    inputs = listOf(LOCAL_SOURCE_REGISTERED_SUPPORT_CONTEXT),
)
private val LEGACY_SOURCE_UNAVAILABLE_BLOCKER = FeatureContextBlocker(
    id = FeatureArtifactId("entry.catalogue.legacy-source-unavailable"),
    inputs = listOf(LEGACY_SOURCE_REGISTERED_SUPPORT_CONTEXT),
)

internal enum class EntryCatalogueBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    SOURCE_DESCRIPTION(FeatureArtifactId("entry.catalogue.source-description.projection")),
    CATALOGUE_AVAILABILITY(FeatureArtifactId("entry.catalogue.availability.projection")),
    LATEST_AVAILABILITY(FeatureArtifactId("entry.catalogue.latest.projection")),
}

internal object EntryCatalogueFeatureContributor : FeatureGraphContributor {
    override val owner = ENTRY_CATALOGUE_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_CATALOGUE_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = SOURCE_DESCRIPTION_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Always,
                        contextInputs = listOf(SOURCE_DESCRIPTION_CONTEXT),
                        contextRule = featureContextRule(owner) { FeatureContextDecision.Applicable },
                        behaviorProjections = listOf(EntryCatalogueBehavior.SOURCE_DESCRIPTION),
                        behavioralContracts = listOf(EntrySourceDescriptionBehaviorContract),
                    ),
                    FeatureIntegration(
                        id = CATALOGUE_AVAILABILITY_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Always,
                        contextInputs = listOf(SOURCE_DESCRIPTION_CONTEXT),
                        contextRule = featureContextRule(owner) { evidence ->
                            if (evidence.value(SOURCE_DESCRIPTION_CONTEXT).description.catalogue != null) {
                                FeatureContextDecision.Applicable
                            } else {
                                FeatureContextDecision.Blocked(listOf(CATALOGUE_UNAVAILABLE_BLOCKER))
                            }
                        },
                        contextBlockers = listOf(CATALOGUE_UNAVAILABLE_BLOCKER),
                        behaviorProjections = listOf(EntryCatalogueBehavior.CATALOGUE_AVAILABILITY),
                        behavioralContracts = listOf(EntryCatalogueAvailabilityBehaviorContract),
                    ),
                    FeatureIntegration(
                        id = LATEST_AVAILABILITY_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Always,
                        contextInputs = listOf(SOURCE_DESCRIPTION_CONTEXT),
                        contextRule = featureContextRule(owner) { evidence ->
                            if (evidence.value(SOURCE_DESCRIPTION_CONTEXT).description.catalogue?.supportsLatest ==
                                true
                            ) {
                                FeatureContextDecision.Applicable
                            } else {
                                FeatureContextDecision.Blocked(listOf(LATEST_UNAVAILABLE_BLOCKER))
                            }
                        },
                        contextBlockers = listOf(LATEST_UNAVAILABLE_BLOCKER),
                        behaviorProjections = listOf(EntryCatalogueBehavior.LATEST_AVAILABILITY),
                        behavioralContracts = listOf(EntryLatestAvailabilityBehaviorContract),
                    ),
                    sourceReferenceIntegration(
                        id = LOCAL_SOURCE_REFERENCE_INTEGRATION_ID,
                        input = LOCAL_SOURCE_REGISTERED_SUPPORT_CONTEXT,
                        blocker = LOCAL_SOURCE_UNAVAILABLE_BLOCKER,
                        reference = LOCAL_SOURCE_REFERENCE,
                    ),
                    sourceReferenceIntegration(
                        id = LEGACY_SOURCE_REFERENCE_INTEGRATION_ID,
                        input = LEGACY_SOURCE_REGISTERED_SUPPORT_CONTEXT,
                        blocker = LEGACY_SOURCE_UNAVAILABLE_BLOCKER,
                        reference = LEGACY_SOURCE_REFERENCE,
                    ),
                ),
            ),
        )
    }
}

private fun sourceReferenceIntegration(
    id: FeatureIntegrationId,
    input: mihon.feature.graph.ContextInputDefinition<Boolean>,
    blocker: FeatureContextBlocker,
    reference: EntryContentTypeReferenceContribution,
) = FeatureIntegration(
    id = id,
    prerequisites = CapabilityExpression.Always,
    contextInputs = listOf(input),
    contextRule = featureContextRule(ENTRY_CATALOGUE_FEATURE_OWNER) { evidence ->
        if (evidence.value(input)) {
            FeatureContextDecision.Applicable
        } else {
            FeatureContextDecision.Blocked(listOf(blocker))
        }
    },
    contextBlockers = listOf(blocker),
    projectionRequirements = listOf(reference.requirement),
    projections = listOf(reference.projection),
)
