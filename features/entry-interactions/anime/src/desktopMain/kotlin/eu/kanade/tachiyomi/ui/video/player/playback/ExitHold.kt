package eu.kanade.tachiyomi.ui.video.player.playback

import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Holds the process's exit until [job] is done, for a few seconds at most, as the desktop app ends its process right
 * after its window closes, which would cut short a save the player started as it closed.
 */
internal fun holdExitUntilDone(job: Job) {
    if (job.isCompleted) return
    val hook = Thread({ runBlocking { withTimeoutOrNull(EXIT_HOLD_MS) { job.join() } } }, "Player progress save")
    try {
        Runtime.getRuntime().addShutdownHook(hook)
    } catch (_: IllegalStateException) {
        // Already exiting, too late to wait
        return
    }
    job.invokeOnCompletion {
        try {
            Runtime.getRuntime().removeShutdownHook(hook)
        } catch (_: IllegalStateException) {
            // Exiting: the hook runs and finds the job done
        }
    }
}

private const val EXIT_HOLD_MS = 5_000L
