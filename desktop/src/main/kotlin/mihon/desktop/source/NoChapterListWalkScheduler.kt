package mihon.desktop.source

import dev.zacsweers.metro.Inject
import tachiyomi.domain.entry.service.ChapterListWalkScheduler

/**
 * Desktop runs no background work yet, so no walk through the older parts of a source's chapter list goes on after a
 * refresh reads its first part, and nothing is scheduled. Stands in until desktop runs background work.
 */
@Inject
class NoChapterListWalkScheduler : ChapterListWalkScheduler {
    override fun schedule(restart: Boolean) = Unit
}
