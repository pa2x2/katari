package mihon.desktop.library

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.data.library.LibraryMetadataUpdater
import mihon.desktop.work.ExistingWorkPolicy
import mihon.desktop.work.InProcessWork
import mihon.desktop.work.InProcessWorkRequest
import mihon.desktop.work.ResumableWork
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.library.service.MetadataUpdateRunner

/** Refreshes the active profile's library details as [InProcessWork], one refresh at a time. */
@Inject
@SingleIn(AppScope::class)
class InProcessMetadataUpdateRunner(
    private val work: InProcessWork,
    private val activeProfileProvider: ActiveProfileProvider,
    // One per refresh, as each keeps its own progress.
    private val updater: () -> LibraryMetadataUpdater,
) : MetadataUpdateRunner, ResumableWork {
    override val kinds: Set<String> = setOf(NAME)

    override suspend fun startNow(): Boolean {
        if (work.currentWorks(NAME).any { it.isRunning }) return false
        start(activeProfileProvider.activeProfileId)
        return true
    }

    override suspend fun stop() {
        work.currentWorks(NAME).filter { it.isRunning }.forEach { work.cancelById(it.id) }
    }

    override suspend fun resume(kind: String, input: Map<String, String>) {
        input[KEY_PROFILE]?.toLongOrNull()?.let(::start)
    }

    private fun start(profileId: Long) {
        work.enqueue(
            NAME,
            ExistingWorkPolicy.Keep,
            InProcessWorkRequest(resumeAs = NAME, input = mapOf(KEY_PROFILE to profileId.toString())) {
                updater().update(profileId)
            },
        )
    }

    private companion object {
        const val NAME = "MetadataUpdate"
        const val KEY_PROFILE = "profile"
    }
}
