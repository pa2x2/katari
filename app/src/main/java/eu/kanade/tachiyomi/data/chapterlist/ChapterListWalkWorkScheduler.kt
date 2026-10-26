package eu.kanade.tachiyomi.data.chapterlist

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import tachiyomi.domain.entry.service.ChapterListWalkScheduler

@Inject
@SingleIn(AppScope::class)
class ChapterListWalkWorkScheduler(private val application: Application) : ChapterListWalkScheduler {
    override fun schedule() = ChapterListWalkJob.start(application, restart = false)
}
