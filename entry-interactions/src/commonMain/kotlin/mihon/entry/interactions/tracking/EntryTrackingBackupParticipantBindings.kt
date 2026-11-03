package mihon.entry.interactions.tracking

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import mihon.entry.interactions.persistence.backup.EntryBackupStateParticipant
import mihon.entry.interactions.tracking.backup.EntryTrackingBackupFeature
import mihon.entry.interactions.tracking.backup.EntryTrackingBackupParticipant

/** Tracking's part in backups, apart from the feature so a host without backups can take the feature alone. */
@ContributesTo(AppScope::class)
@BindingContainer
object EntryTrackingBackupParticipantBindings {
    @Provides
    @IntoSet
    internal fun backupState(
        backupFeature: () -> EntryTrackingBackupFeature,
    ): EntryBackupStateParticipant = EntryTrackingBackupParticipant(backupFeature)
}
