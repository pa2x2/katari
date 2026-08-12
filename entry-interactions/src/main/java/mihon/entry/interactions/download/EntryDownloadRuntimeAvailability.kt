package mihon.entry.interactions.download

import kotlinx.coroutines.CompletableDeferred

/** Process-local barrier between eager WorkManager restoration and runtime dependency registration. */
internal object EntryDownloadRuntimeAvailability {
    private val installed = CompletableDeferred<Unit>()

    fun markInstalled() {
        installed.complete(Unit)
    }

    suspend fun awaitInstalled() {
        installed.await()
    }
}

/** Called by the application composition root after every download worker dependency is registered. */
fun markEntryDownloadRuntimeDependenciesRegistered() {
    EntryDownloadRuntimeAvailability.markInstalled()
}
