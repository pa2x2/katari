package app.katari.sync.client.run

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds

/**
 * Told after each run that synced, having pulled everything the server had, so what waits on records from other
 * devices, such as a message about an entry that hadn't arrived, can look again. It is told within the run, so it
 * hands any lasting work off; a listener's failure is logged and never fails the sync.
 */
fun interface SyncedListener {
    fun onSynced()
}

/** Features that wait on records contribute their listeners; a graph may have none. */
@ContributesTo(AppScope::class)
interface SyncedListenerBindings {
    @Multibinds(allowEmpty = true)
    fun syncedListeners(): Set<SyncedListener>
}
