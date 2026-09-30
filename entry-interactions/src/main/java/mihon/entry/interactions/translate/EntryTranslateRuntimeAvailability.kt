package mihon.entry.interactions.translate

import kotlinx.coroutines.CompletableDeferred

/** Process-local barrier between WorkManager restoring the worker and its dependencies being registered. */
internal object EntryTranslateRuntimeAvailability {
    private val installed = CompletableDeferred<Unit>()

    fun markInstalled() {
        installed.complete(Unit)
    }

    suspend fun awaitInstalled() {
        installed.await()
    }
}

/** Called by the application composition root after every translation worker dependency is registered. */
fun markEntryTranslateRuntimeDependenciesRegistered() {
    EntryTranslateRuntimeAvailability.markInstalled()
}
