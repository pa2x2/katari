package mihon.entry.interactions.tracking

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.entry.interactions.tracking.backup.DefaultEntryTrackingBackupFeature
import mihon.entry.interactions.tracking.backup.EntryTrackingBackupFeature
import mihon.entry.interactions.tracking.host.EntryTrackingHost

@ContributesTo(AppScope::class)
@BindingContainer
object EntryTrackingBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun feature(trackingHost: EntryTrackingHost): EntryTrackingFeature =
        DefaultEntryTrackingFeature(
            host = trackingHost,
        )

    @Provides
    @SingleIn(AppScope::class)
    fun backupFeature(trackingHost: EntryTrackingHost): EntryTrackingBackupFeature =
        DefaultEntryTrackingBackupFeature(trackingHost.backup)
}
