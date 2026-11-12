package app.katari.sync.client.downloadrequest

import app.katari.sync.client.run.SyncedListener
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

/** After each run that synced, the download requests waiting look for their entry again. */
@Inject
@ContributesIntoSet(AppScope::class)
class DownloadRequestRetry(private val inbox: DownloadRequestInbox) : SyncedListener {
    override fun onSynced() {
        inbox.lookAgain()
    }
}
