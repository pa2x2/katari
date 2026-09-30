package mihon.entry.interactions.translate.ahead

import mihon.entry.interactions.download.ENTRY_DOWNLOAD_MEDIA_SESSION_PARTICIPANT
import mihon.entry.interactions.download.EntryDownloadCapability
import mihon.entry.interactions.media.EntryMediaSessionCapability
import mihon.entry.interactions.media.session.ENTRY_MEDIA_SESSION_CONSEQUENCE_EXECUTION_POINT
import mihon.entry.interactions.media.session.EntryMediaSessionEvent
import mihon.entry.interactions.media.session.EntryMediaSessionExecutionEvent
import mihon.entry.interactions.translate.EntryTranslateCapability
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.allOf
import mihon.feature.graph.execution.FeatureExecutionHandler
import mihon.feature.graph.execution.FeatureExecutionOrder
import mihon.feature.graph.execution.FeatureExecutionParticipantBinding
import mihon.feature.graph.execution.FeatureExecutionParticipantDefinition
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

private val FEATURE_OWNER = ContributionOwner("entry-translate-ahead")

/** Reading progress reaches translate ahead after download ahead has queued what it downloads. */
internal object EntryTranslateAheadBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.translate.ahead.media-session.behavior")
}

internal val ENTRY_TRANSLATE_AHEAD_MEDIA_SESSION_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.translate.ahead.media-session"),
    owner = FEATURE_OWNER,
    point = ENTRY_MEDIA_SESSION_CONSEQUENCE_EXECUTION_POINT,
    prerequisites = allOf(
        CapabilityExpression.Provided(EntryMediaSessionCapability.definition),
        CapabilityExpression.Provided(EntryTranslateCapability.definition),
        CapabilityExpression.Provided(EntryDownloadCapability.definition),
    ),
    order = FeatureExecutionOrder(after = setOf(ENTRY_DOWNLOAD_MEDIA_SESSION_PARTICIPANT.id)),
    behavioralContracts = listOf(EntryTranslateAheadBehaviorContract),
)

internal object EntryTranslateAheadContributor : FeatureGraphContributor {
    override val owner = FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_TRANSLATE_AHEAD_MEDIA_SESSION_PARTICIPANT)
    }
}

/** Receives how far the reader got through a chapter. */
internal fun interface EntryTranslateAheadTrigger {
    suspend fun onProgressed(visibleEntry: Entry, child: EntryChapter, fraction: Double, deduplicateByNumber: Boolean)
}

internal fun entryTranslateAheadMediaSessionBinding(
    trigger: () -> EntryTranslateAheadTrigger,
) = FeatureExecutionParticipantBinding(
    definition = ENTRY_TRANSLATE_AHEAD_MEDIA_SESSION_PARTICIPANT,
    handler = FeatureExecutionHandler<EntryMediaSessionExecutionEvent> { execution ->
        val event = execution.event as? EntryMediaSessionEvent.Progressed ?: return@FeatureExecutionHandler
        val fraction = event.fraction ?: return@FeatureExecutionHandler
        trigger().onProgressed(event.visibleEntry, event.child, fraction, event.deduplicateDownloadByNumber)
    },
)
