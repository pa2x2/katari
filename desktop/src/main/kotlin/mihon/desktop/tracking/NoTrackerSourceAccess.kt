package mihon.desktop.tracking

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.track.source.TrackerSourceAccess
import eu.kanade.tachiyomi.data.track.source.TrackerSourceConnection
import eu.kanade.tachiyomi.data.track.source.TrackerSourceSettings

/**
 * Extension sources as desktop has them: none, so the self-hosted trackers (Kavita, Suwayomi) find no server to use,
 * as on Android when their extension isn't installed. Stands in until desktop hosts extensions.
 */
@Inject
class NoTrackerSourceAccess : TrackerSourceAccess {
    override fun settings(sourceKey: Long): TrackerSourceSettings? = null

    override fun connection(sourceKey: Long): TrackerSourceConnection? = null
}
