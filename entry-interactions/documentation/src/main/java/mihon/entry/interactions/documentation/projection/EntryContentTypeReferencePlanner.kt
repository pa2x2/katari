package mihon.entry.interactions.documentation.projection

import mihon.entry.interactions.documentation.EntryContentTypeReferenceContextEvidenceProvider
import mihon.entry.interactions.documentation.EntryContentTypeReferenceElement
import mihon.entry.interactions.documentation.EntryContentTypeReferenceNote
import mihon.entry.interactions.documentation.EntryContentTypeReferenceProjection
import mihon.entry.interactions.documentation.EntryContentTypeReferenceProjectionInput
import mihon.entry.interactions.documentation.EntryContentTypeReferenceProjectionResult
import mihon.entry.interactions.documentation.EntryContentTypeReferenceRow
import mihon.entry.interactions.documentation.EntryContentTypeReferenceSelection
import mihon.entry.interactions.documentation.EntryContentTypeReferenceStatus
import mihon.feature.graph.ApplicableFeatureContext
import mihon.feature.graph.BlockedFeatureContext
import mihon.feature.graph.ConditionalFeatureIntegration
import mihon.feature.graph.FeatureGraph
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.FeatureProjectionSelection
import mihon.feature.graph.IncompleteFeatureContext
import mihon.feature.graph.MissingFeatureContextEvidence
import mihon.feature.graph.resolveFeatureContext
import mihon.feature.graph.selectContextualFeatureArtifacts
import mihon.feature.graph.selectFeatureArtifacts
import mihon.feature.graph.validation.projection.classifyFeatureProjectionParticipation

fun planEntryContentTypeReference(
    graph: FeatureGraph,
    evaluation: FeatureGraphEvaluation,
    contextEvidence: EntryContentTypeReferenceContextEvidenceProvider,
): EntryContentTypeReferencePlan {
    val participation = classifyFeatureProjectionParticipation(
        graph,
        EntryContentTypeReferenceProjection::class,
    )
    val issues = mutableListOf<EntryContentTypeReferenceIssue>()
    participation.missing.forEach { missing ->
        issues += EntryContentTypeReferenceIssue(
            responsibleOwner = missing.responsibleOwner,
            subject = null,
            details = "Feature ${missing.feature.value} does not include or exclude the content-type reference",
        )
    }
    participation.missingImplementations.forEach { missing ->
        issues += EntryContentTypeReferenceIssue(
            responsibleOwner = missing.responsibleOwner,
            subject = null,
            details = "Feature ${missing.feature.value} integration ${missing.integration.value} does not implement " +
                missing.definition.id.value,
        )
    }
    val selections = mutableListOf<FeatureProjectionSelection>()
    selectFeatureArtifacts(graph, evaluation).projections.contentReferenceSelections().forEach { selection ->
        if (selection.contentReferenceProjection().selection ==
            EntryContentTypeReferenceSelection.CONDITIONAL_RELATIONSHIP
        ) {
            issues += EntryContentTypeReferenceIssue(
                responsibleOwner = selection.subject.featureOwner,
                subject = selection.subject,
                details = "Conditional content-reference projection was selected from a static relationship",
            )
        } else {
            selections += selection
        }
    }
    evaluation.integrations
        .filterIsInstance<ConditionalFeatureIntegration>()
        .filter { candidate ->
            candidate.integration.projectionRequirements.any { requirement ->
                requirement.projectionType == EntryContentTypeReferenceProjection::class
            }
        }
        .forEach { candidate ->
            val declaredSelections = candidate.integration.projections
                .filter { projection ->
                    projection.definition.projectionType == EntryContentTypeReferenceProjection::class
                }
                .map { projection ->
                    FeatureProjectionSelection(
                        subject = candidate.subject,
                        projection = projection,
                        matchedProviders = candidate.matchedProviders,
                        suppliedAdapters = candidate.suppliedAdapters,
                        contextEvidence = emptyList(),
                    )
                }
            selections += declaredSelections.filter { selection ->
                selection.contentReferenceProjection().selection ==
                    EntryContentTypeReferenceSelection.CONDITIONAL_RELATIONSHIP
            }
            val requiresApplicableRelationship = declaredSelections.any { selection ->
                selection.contentReferenceProjection().selection ==
                    EntryContentTypeReferenceSelection.APPLICABLE_RELATIONSHIP
            }
            if (!requiresApplicableRelationship) return@forEach

            val evidence = candidate.integration.contextInputs.mapNotNull { input ->
                contextEvidence.evidence(candidate.subject, input)
            }
            if (evidence.size != candidate.integration.contextInputs.size) {
                val supplied = evidence.mapTo(mutableSetOf()) { it.input }
                candidate.integration.contextInputs
                    .filter { it !in supplied }
                    .forEach { missing ->
                        issues += EntryContentTypeReferenceIssue(
                            responsibleOwner = missing.owner,
                            subject = candidate.subject,
                            details = "Missing content-reference evidence ${missing.id.value}",
                        )
                    }
                return@forEach
            }
            val resolved = resolveFeatureContext(
                evaluation = evaluation,
                contentType = candidate.subject.contentType,
                feature = candidate.subject.feature,
                integration = candidate.subject.integration,
                evidence = evidence,
            )
            when (val contextual = resolved.integration) {
                is ApplicableFeatureContext -> {
                    val artifacts = selectContextualFeatureArtifacts(graph, evaluation, resolved)
                    selections += artifacts.projections.contentReferenceSelections().filter { selection ->
                        selection.contentReferenceProjection().selection ==
                            EntryContentTypeReferenceSelection.APPLICABLE_RELATIONSHIP
                    }
                    artifacts.obligations.forEach { obligation ->
                        issues += EntryContentTypeReferenceIssue(
                            responsibleOwner = obligation.responsibleOwner,
                            subject = candidate.subject,
                            details = "Unresolved projection obligation: $obligation",
                        )
                    }
                }
                is BlockedFeatureContext -> Unit
                is IncompleteFeatureContext -> contextual.obligations.forEach { obligation ->
                    issues += EntryContentTypeReferenceIssue(
                        responsibleOwner = obligation.responsibleOwner,
                        subject = candidate.subject,
                        details = "Incomplete contextual projection: $obligation",
                    )
                }
                is MissingFeatureContextEvidence -> issues += EntryContentTypeReferenceIssue(
                    responsibleOwner = candidate.subject.featureOwner,
                    subject = candidate.subject,
                    details = "Context resolution rejected supplied documentation evidence",
                )
            }
        }

    val elements = declaredElements(graph, issues)
    val rowStatuses = mutableMapOf<Pair<String, mihon.feature.graph.ContentTypeId>, EntryContentTypeReferenceStatus>()
    val noteTypes = mutableMapOf<String, MutableSet<mihon.feature.graph.ContentTypeId>>()
    selections.forEach { selection ->
        val projection = selection.projection.implementation as EntryContentTypeReferenceProjection
        val result = projection.project(
            EntryContentTypeReferenceProjectionInput(
                subject = selection.subject,
                matchedProviders = selection.matchedProviders,
                suppliedAdapters = selection.suppliedAdapters,
                contextEvidence = selection.contextEvidence,
            ),
        )
        if (
            projection.selection == EntryContentTypeReferenceSelection.CONDITIONAL_RELATIONSHIP &&
            result is EntryContentTypeReferenceProjectionResult.Cell &&
            result.status != EntryContentTypeReferenceStatus.SOURCE_DEPENDENT
        ) {
            issues += EntryContentTypeReferenceIssue(
                responsibleOwner = selection.subject.featureOwner,
                subject = selection.subject,
                details = "Conditional content-reference row ${projection.element.id} must be source-dependent",
            )
            return@forEach
        }
        when (val element = projection.element) {
            is EntryContentTypeReferenceRow -> when (result) {
                is EntryContentTypeReferenceProjectionResult.Cell -> {
                    val key = element.id to selection.subject.contentType
                    val previous = rowStatuses.put(key, result.status)
                    if (previous != null && previous != result.status) {
                        issues += EntryContentTypeReferenceIssue(
                            responsibleOwner = selection.subject.featureOwner,
                            subject = selection.subject,
                            details = "Conflicting statuses for content-reference row ${element.id}",
                        )
                    }
                }
                EntryContentTypeReferenceProjectionResult.IncludedNote -> issues += kindMismatch(selection, element)
            }
            is EntryContentTypeReferenceNote -> when (result) {
                EntryContentTypeReferenceProjectionResult.IncludedNote ->
                    noteTypes.getOrPut(element.id, ::mutableSetOf) += selection.subject.contentType
                is EntryContentTypeReferenceProjectionResult.Cell -> issues += kindMismatch(selection, element)
            }
        }
    }

    return EntryContentTypeReferencePlan(
        contentTypes = graph.contentTypes.map { it.contentType },
        rows = elements.filterIsInstance<EntryContentTypeReferenceRow>()
            .sortedWith(
                compareBy({
                    it.section.order
                }, EntryContentTypeReferenceRow::order, EntryContentTypeReferenceRow::id),
            )
            .map { row ->
                EntryContentTypeReferencePlannedRow(
                    definition = row,
                    statuses = graph.contentTypes.mapNotNull { type ->
                        rowStatuses[row.id to type.contentType]?.let { type.contentType to it }
                    }.toMap(),
                )
            },
        notes = elements.filterIsInstance<EntryContentTypeReferenceNote>()
            .filter { it.id in noteTypes }
            .sortedWith(
                compareBy({
                    it.section.order
                }, EntryContentTypeReferenceNote::order, EntryContentTypeReferenceNote::id),
            )
            .map { note -> EntryContentTypeReferencePlannedNote(note, noteTypes.getValue(note.id)) },
        issues = issues,
    )
}

private fun List<FeatureProjectionSelection>.contentReferenceSelections() = filter { selection ->
    selection.projection.definition.projectionType == EntryContentTypeReferenceProjection::class
}

private fun FeatureProjectionSelection.contentReferenceProjection() =
    projection.implementation as EntryContentTypeReferenceProjection

private fun declaredElements(
    graph: FeatureGraph,
    issues: MutableList<EntryContentTypeReferenceIssue>,
): List<EntryContentTypeReferenceElement> {
    return graph.features
        .flatMap { feature ->
            feature.integrations.flatMap { integration ->
                integration.projections.mapNotNull { projection ->
                    (projection.implementation as? EntryContentTypeReferenceProjection)?.let {
                        Triple(feature.owner, integration.id, it.element)
                    }
                }
            }
        }
        .groupBy { it.third.id }
        .map { (id, declarations) ->
            val elements = declarations.map { it.third }.distinct()
            if (elements.size != 1) {
                declarations.forEach { declaration ->
                    issues += EntryContentTypeReferenceIssue(
                        responsibleOwner = declaration.first,
                        subject = null,
                        details = "Conflicting content-reference element $id on ${declaration.second.value}",
                    )
                }
            }
            elements.first()
        }
}

private fun kindMismatch(
    selection: FeatureProjectionSelection,
    element: EntryContentTypeReferenceElement,
) = EntryContentTypeReferenceIssue(
    responsibleOwner = selection.subject.featureOwner,
    subject = selection.subject,
    details = "Content-reference projection ${element.id} returned a result for the wrong element kind",
)
