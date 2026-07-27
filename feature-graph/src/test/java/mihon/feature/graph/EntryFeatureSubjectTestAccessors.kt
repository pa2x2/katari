package mihon.feature.graph

internal val FeatureIntegrationSubject.entryContentType: ContentTypeId
    get() = (affectedSubject.id as FeatureSubjectId.EntryContentType).contentType

internal val FeatureGraph.entryContentTypes: List<ContentTypeContribution>
    get() = subjects.filterIsInstance<ContentTypeContribution>()
