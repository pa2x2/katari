package mihon.feature.graph.validation.projection

import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureGraph
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.FeatureProjectionDefinition
import mihon.feature.graph.FeatureSubjectScope
import kotlin.reflect.KClass

/** One feature's explicit relationship with an optional projection channel. */
sealed interface FeatureProjectionParticipation {
    val feature: FeatureId
    val owner: ContributionOwner

    data class Included(
        override val feature: FeatureId,
        override val owner: ContributionOwner,
        val requirements: List<FeatureProjectionRequirementReference>,
    ) : FeatureProjectionParticipation

    data class Excluded(
        override val feature: FeatureId,
        override val owner: ContributionOwner,
        val reason: String,
    ) : FeatureProjectionParticipation
}

data class FeatureProjectionRequirementReference(
    val integration: FeatureIntegrationId,
    val definition: FeatureProjectionDefinition<*>,
)

data class MissingFeatureProjectionParticipation(
    val feature: FeatureId,
    val responsibleOwner: ContributionOwner,
)

data class MissingFeatureProjectionImplementation(
    val feature: FeatureId,
    val responsibleOwner: ContributionOwner,
    val integration: FeatureIntegrationId,
    val definition: FeatureProjectionDefinition<*>,
)

data class FeatureProjectionParticipationResult(
    val participation: List<FeatureProjectionParticipation>,
    val missing: List<MissingFeatureProjectionParticipation>,
    val missingImplementations: List<MissingFeatureProjectionImplementation>,
) {
    val isComplete: Boolean
        get() = missing.isEmpty() && missingImplementations.isEmpty()
}

/**
 * Classifies every discovered Feature participating in [subjectScope] for one optional projection channel.
 *
 * Inclusion is inferred from feature-owned projection requirements. Exclusion must be explicit and justified. The
 * result never enumerates known Features and therefore reports an unknown future Feature automatically. A null scope
 * retains whole-graph classification for scope-neutral projection channels.
 */
fun <P : Any> classifyFeatureProjectionParticipation(
    graph: FeatureGraph,
    projectionType: KClass<P>,
    subjectScope: FeatureSubjectScope? = null,
): FeatureProjectionParticipationResult {
    val participatingFeatures = graph.features.filter { feature ->
        subjectScope == null || feature.integrations.any { it.subjectScope == subjectScope }
    }
    val classified = participatingFeatures.mapNotNull { feature ->
        val requirements = feature.integrations
            .filter { subjectScope == null || it.subjectScope == subjectScope }
            .flatMap { integration ->
                integration.projectionRequirements
                    .filter { it.projectionType == projectionType }
                    .map { definition -> FeatureProjectionRequirementReference(integration.id, definition) }
            }
        val exclusion = feature.projectionExclusions.singleOrNull { it.projectionType == projectionType }
        when {
            requirements.isNotEmpty() -> FeatureProjectionParticipation.Included(
                feature = feature.feature,
                owner = feature.owner,
                requirements = requirements.sortedWith(
                    compareBy(
                        { it.integration.value },
                        { it.definition.id.value },
                    ),
                ),
            )
            exclusion != null -> FeatureProjectionParticipation.Excluded(
                feature = feature.feature,
                owner = feature.owner,
                reason = exclusion.reason,
            )
            else -> null
        }
    }
    val classifiedFeatures = classified.mapTo(mutableSetOf()) { it.feature }
    return FeatureProjectionParticipationResult(
        participation = classified.sortedBy { it.feature.value },
        missing = participatingFeatures
            .filter { it.feature !in classifiedFeatures }
            .map { MissingFeatureProjectionParticipation(it.feature, it.owner) },
        missingImplementations = participatingFeatures.flatMap { feature ->
            feature.integrations
                .filter { subjectScope == null || it.subjectScope == subjectScope }
                .flatMap { integration ->
                    val suppliedIds = integration.projections
                        .filter { it.definition.projectionType == projectionType }
                        .mapTo(mutableSetOf()) { it.definition.id }
                    integration.projectionRequirements
                        .filter { it.projectionType == projectionType && it.id !in suppliedIds }
                        .map { definition ->
                            MissingFeatureProjectionImplementation(
                                feature = feature.feature,
                                responsibleOwner = feature.owner,
                                integration = integration.id,
                                definition = definition,
                            )
                        }
                }
        },
    )
}
