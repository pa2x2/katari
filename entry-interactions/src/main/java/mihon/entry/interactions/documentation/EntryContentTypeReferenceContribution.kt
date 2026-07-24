package mihon.entry.interactions.documentation

import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureProjection
import mihon.feature.graph.FeatureProjectionDefinition
import mihon.feature.graph.featureProjectionDefinition

internal data class EntryContentTypeReferenceContribution(
    val requirement: FeatureProjectionDefinition<EntryContentTypeReferenceProjection>,
    val projection: FeatureProjection<EntryContentTypeReferenceProjection>,
)

internal fun entryContentTypeReferenceContribution(
    id: String,
    owner: ContributionOwner,
    section: EntryContentTypeReferenceSection,
    label: String,
    order: Int,
    selection: EntryContentTypeReferenceSelection = EntryContentTypeReferenceSelection.APPLICABLE_RELATIONSHIP,
    project: (EntryContentTypeReferenceProjectionInput) -> EntryContentTypeReferenceStatus = {
        EntryContentTypeReferenceStatus.SUPPORTED
    },
): EntryContentTypeReferenceContribution {
    val requirement = featureProjectionDefinition<EntryContentTypeReferenceProjection>(
        id = FeatureArtifactId("content-type-reference.$id"),
        owner = owner,
    )
    val implementation = object : EntryContentTypeReferenceProjection {
        override val element = EntryContentTypeReferenceRow(id, section, label, order)
        override val selection = selection

        override fun project(input: EntryContentTypeReferenceProjectionInput) =
            EntryContentTypeReferenceProjectionResult.Cell(project(input))
    }
    return EntryContentTypeReferenceContribution(
        requirement = requirement,
        projection = FeatureProjection(requirement, implementation),
    )
}

internal fun entryContentTypeReferenceNoteContribution(
    id: String,
    owner: ContributionOwner,
    section: EntryContentTypeReferenceSection,
    text: String,
    order: Int,
    selection: EntryContentTypeReferenceSelection = EntryContentTypeReferenceSelection.APPLICABLE_RELATIONSHIP,
): EntryContentTypeReferenceContribution {
    val requirement = featureProjectionDefinition<EntryContentTypeReferenceProjection>(
        id = FeatureArtifactId("content-type-reference.$id"),
        owner = owner,
    )
    val implementation = object : EntryContentTypeReferenceProjection {
        override val element = EntryContentTypeReferenceNote(id, section, text, order)
        override val selection = selection

        override fun project(input: EntryContentTypeReferenceProjectionInput) =
            EntryContentTypeReferenceProjectionResult.IncludedNote
    }
    return EntryContentTypeReferenceContribution(
        requirement = requirement,
        projection = FeatureProjection(requirement, implementation),
    )
}
