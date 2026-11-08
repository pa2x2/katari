package mihon.entry.interactions.tracking

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import mihon.entry.interactions.durable.EntryDurableParticipant
import mihon.entry.interactions.merge.consequence.EntryMergeDurableEvent
import mihon.entry.interactions.merge.host.EntryMergeHost
import mihon.entry.interactions.tracking.merge.EntryTrackingMergeParticipant
import tachiyomi.domain.entry.model.Entry

/** Tracking's part in merges, apart from the feature so a host without merges can take the feature alone. */
@ContributesTo(AppScope::class)
@BindingContainer
object EntryTrackingMergeParticipantBindings {
    @Provides
    @IntoSet
    internal fun mergeParticipant(
        mergeHost: EntryMergeHost,
        feature: () -> EntryTrackingFeature,
    ): EntryDurableParticipant<EntryMergeDurableEvent> = EntryTrackingMergeParticipant(
        resolveEntry = { profileId, type, sourceId, url ->
            mergeHost.profile(profileId).resolveEntryIdentity(
                Entry.create().copy(
                    profileId = profileId,
                    type = type,
                    source = sourceId,
                    url = url,
                ),
            )
        },
        feature = feature,
    )
}
