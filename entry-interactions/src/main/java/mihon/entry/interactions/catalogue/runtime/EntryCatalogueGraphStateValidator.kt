package mihon.entry.interactions.catalogue.runtime

import mihon.entry.interactions.catalogue.CATALOGUE_AVAILABILITY_INTEGRATION_ID
import mihon.entry.interactions.catalogue.ENTRY_CATALOGUE_FEATURE_ID
import mihon.entry.interactions.catalogue.EntryCatalogueBehavior
import mihon.entry.interactions.catalogue.LATEST_AVAILABILITY_INTEGRATION_ID
import mihon.entry.interactions.catalogue.SOURCE_DESCRIPTION_CONTEXT
import mihon.entry.interactions.catalogue.SOURCE_DESCRIPTION_INTEGRATION_ID
import mihon.entry.interactions.catalogue.SourceDescriptionEvidence
import mihon.entry.interactions.entryContentType
import mihon.entry.interactions.resolveFeatureContext
import mihon.feature.graph.ApplicableFeatureContext
import mihon.feature.graph.BlockedFeatureContext
import mihon.feature.graph.ContextEvidence
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.contextEvidence
import tachiyomi.domain.source.model.EntrySourceDescription

internal class EntryCatalogueGraphStateValidator(
    private val evaluation: FeatureGraphEvaluation,
) {
    fun validate(description: EntrySourceDescription) {
        val evidence = contextEvidence(
            SOURCE_DESCRIPTION_CONTEXT,
            SourceDescriptionEvidence(description),
        )
        requireUniformState(
            SOURCE_DESCRIPTION_INTEGRATION_ID,
            EntryCatalogueBehavior.SOURCE_DESCRIPTION,
            evidence,
            applicable = true,
        )
        requireUniformState(
            CATALOGUE_AVAILABILITY_INTEGRATION_ID,
            EntryCatalogueBehavior.CATALOGUE_AVAILABILITY,
            evidence,
            applicable = description.catalogue != null,
        )
        requireUniformState(
            LATEST_AVAILABILITY_INTEGRATION_ID,
            EntryCatalogueBehavior.LATEST_AVAILABILITY,
            evidence,
            applicable = description.catalogue?.supportsLatest == true,
        )
    }

    private fun requireUniformState(
        integration: FeatureIntegrationId,
        behaviorProjection: EntryCatalogueBehavior,
        evidence: ContextEvidence<SourceDescriptionEvidence>,
        applicable: Boolean,
    ) {
        val subjects = evaluation.integrations
            .map { it.subject }
            .filter { it.feature == ENTRY_CATALOGUE_FEATURE_ID && it.integration == integration }
        check(subjects.isNotEmpty()) { "Entry Catalogue integration $integration was not discovered" }

        subjects.forEach { subject ->
            val resolution = resolveFeatureContext(
                evaluation = evaluation,
                contentType = subject.entryContentType,
                feature = ENTRY_CATALOGUE_FEATURE_ID,
                integration = integration,
                evidence = listOf(evidence),
            )
            val hasBehavior = resolution.behaviorProjections.any { it.projection.id == behaviorProjection.id }
            check(
                if (applicable) {
                    resolution.integration is ApplicableFeatureContext && hasBehavior
                } else {
                    resolution.integration is BlockedFeatureContext && !hasBehavior
                },
            ) {
                "Entry Catalogue integration $integration resolved inconsistently for ${subject.entryContentType}: " +
                    "${resolution.integration}, behaviors=${resolution.behaviorProjections}"
            }
        }
    }
}
