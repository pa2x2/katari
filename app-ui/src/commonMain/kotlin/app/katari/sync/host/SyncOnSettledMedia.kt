package app.katari.sync.host

import app.katari.sync.client.trigger.SyncTriggers
import app.katari.sync.ui.handoff.SettledReading
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import mihon.entry.interactions.media.session.EntryMediaSessionSettledListener

/** Sends progress at once when a reader or the player puts its media down (D021). */
@Inject
@ContributesIntoSet(AppScope::class)
class SyncOnSettledMedia(
    private val triggers: SyncTriggers,
    private val settledReading: SettledReading,
) : EntryMediaSessionSettledListener {
    override fun onSettled(entryId: Long) {
        settledReading.record(entryId)
        triggers.readingRecorded()
    }
}
