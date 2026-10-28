package mihon.desktop.source

import dev.zacsweers.metro.Inject
import tachiyomi.domain.entry.service.ChapterListWalkScheduler

/**
 * Desktop reads no chapter lists from sources yet, so no walk through their older parts goes on in the background and
 * nothing is scheduled. Stands in until desktop hosts extensions and runs background work.
 */
@Inject
class NoChapterListWalkScheduler : ChapterListWalkScheduler {
    override fun schedule(restart: Boolean) = Unit
}
