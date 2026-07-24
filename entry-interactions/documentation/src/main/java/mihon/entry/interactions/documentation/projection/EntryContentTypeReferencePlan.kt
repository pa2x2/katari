package mihon.entry.interactions.documentation.projection

import mihon.entry.interactions.documentation.EntryContentTypeReferenceNote
import mihon.entry.interactions.documentation.EntryContentTypeReferenceRow
import mihon.entry.interactions.documentation.EntryContentTypeReferenceStatus
import mihon.feature.graph.ContentTypeId
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureIntegrationSubject

data class EntryContentTypeReferencePlan(
    val contentTypes: List<ContentTypeId>,
    val rows: List<EntryContentTypeReferencePlannedRow>,
    val notes: List<EntryContentTypeReferencePlannedNote>,
    val issues: List<EntryContentTypeReferenceIssue>,
) {
    val isComplete: Boolean
        get() = issues.isEmpty()
}

data class EntryContentTypeReferencePlannedRow(
    val definition: EntryContentTypeReferenceRow,
    val statuses: Map<ContentTypeId, EntryContentTypeReferenceStatus>,
)

data class EntryContentTypeReferencePlannedNote(
    val definition: EntryContentTypeReferenceNote,
    val applicableContentTypes: Set<ContentTypeId>,
)

data class EntryContentTypeReferenceIssue(
    val responsibleOwner: ContributionOwner,
    val subject: FeatureIntegrationSubject?,
    val details: String,
)
