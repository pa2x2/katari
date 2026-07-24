package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.feature.graph.FeatureExecutionRuntime
import mihon.feature.graph.FeatureGraphEvaluation

internal class DefaultEntryMediaSessionFeature(
    evaluation: FeatureGraphEvaluation,
    private val executions: FeatureExecutionRuntime,
) : EntryMediaSessionFeature {
    private val applicableTypes = evaluation.applicableProviderTypes<EntryMediaSessionProcessor>(
        feature = ENTRY_MEDIA_SESSION_FEATURE_ID,
        integration = ENTRY_MEDIA_SESSION_INTEGRATION_ID,
        behaviorProjection = mihon.feature.graph.FeatureArtifactId("entry.media-session.event-emission"),
    )

    override fun isApplicable(type: EntryType): Boolean = type in applicableTypes

    override suspend fun onEvent(event: EntryMediaSessionEvent): EntryMediaSessionResult {
        val type = event.visibleEntry.type
        if (!isApplicable(type)) return EntryMediaSessionResult.Inapplicable(type)

        val executionEvent = EntryMediaSessionExecutionEvent(event)
        executions.executeInline(
            point = ENTRY_MEDIA_SESSION_POLICY_EXECUTION_POINT,
            contentType = type.toContentTypeId(),
            event = executionEvent,
        ).throwFirstFailure()
        executions.executeInline(
            point = ENTRY_MEDIA_SESSION_CONSEQUENCE_EXECUTION_POINT,
            contentType = type.toContentTypeId(),
            event = executionEvent,
        ).throwFirstFailure()
        return EntryMediaSessionResult.Handled
    }
}
