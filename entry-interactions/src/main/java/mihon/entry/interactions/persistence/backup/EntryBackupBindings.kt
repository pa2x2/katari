package mihon.entry.interactions.persistence.backup

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
@BindingContainer
object EntryBackupBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun feature(participants: Set<EntryBackupStateParticipant>): EntryBackupFeature =
        EntryBackupCoordinator(participants)
}
