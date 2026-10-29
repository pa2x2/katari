package mihon.desktop.tracking

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.track.source.TrackerSourceAccess
import eu.kanade.tachiyomi.data.track.source.TrackerSourceConnection
import eu.kanade.tachiyomi.data.track.source.TrackerSourceSettings

/**
 * Desktop hosts no APK extensions yet, and the self-hosted trackers (Kavita, Suwayomi) find their server through
 * theirs, so they find none, as on Android when their extension isn't installed. Stands in until desktop loads APK
 * extensions.
 */
@Inject
class NoTrackerSourceAccess : TrackerSourceAccess {
    override fun settings(sourceKey: Long): TrackerSourceSettings? = null

    override fun connection(sourceKey: Long): TrackerSourceConnection? = null
}
