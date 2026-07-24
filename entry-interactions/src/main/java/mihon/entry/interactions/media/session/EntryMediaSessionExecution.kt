package mihon.entry.interactions

import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureExecutionFailurePolicy
import mihon.feature.graph.FeatureExecutionPointId
import mihon.feature.graph.inlineFeatureExecutionPointDefinition

internal val ENTRY_MEDIA_SESSION_OWNER = ContributionOwner("entry-media-session")

internal val ENTRY_MEDIA_SESSION_POLICY_EXECUTION_POINT =
    inlineFeatureExecutionPointDefinition<EntryMediaSessionExecutionEvent>(
        id = FeatureExecutionPointId("entry.media-session.policy"),
        owner = ENTRY_MEDIA_SESSION_OWNER,
        failurePolicy = FeatureExecutionFailurePolicy.FAIL_FAST,
    )

internal val ENTRY_MEDIA_SESSION_CONSEQUENCE_EXECUTION_POINT =
    inlineFeatureExecutionPointDefinition<EntryMediaSessionExecutionEvent>(
        id = FeatureExecutionPointId("entry.media-session.consequences"),
        owner = ENTRY_MEDIA_SESSION_OWNER,
        failurePolicy = FeatureExecutionFailurePolicy.FAIL_FAST,
    )

internal class EntryMediaSessionExecutionEvent(
    val event: EntryMediaSessionEvent,
) {
    private val blockedConsequences = mutableSetOf<EntryMediaSessionConsequence>()

    var progressResult: EntryProgressRecordingResult? = null

    fun block(consequences: Set<EntryMediaSessionConsequence>) {
        blockedConsequences += consequences
    }

    fun permits(consequence: EntryMediaSessionConsequence): Boolean = consequence !in blockedConsequences
}
