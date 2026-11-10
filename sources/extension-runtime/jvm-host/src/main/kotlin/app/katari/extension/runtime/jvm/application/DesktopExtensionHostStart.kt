package app.katari.extension.runtime.jvm.application

import eu.kanade.tachiyomi.extension.platform.ExtensionHostStart
import kotlinx.coroutines.CompletableDeferred

/**
 * Started by the desktop app once the Android stand-ins, the main looper and Injekt are set up, which it does only
 * after restoring the active profile whose source settings extensions read.
 */
class DesktopExtensionHostStart : ExtensionHostStart {

    private val started = CompletableDeferred<Unit>()

    fun markStarted() {
        started.complete(Unit)
    }

    override suspend fun awaitStarted() = started.await()
}
