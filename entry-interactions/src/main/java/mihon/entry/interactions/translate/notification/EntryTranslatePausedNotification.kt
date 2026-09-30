package mihon.entry.interactions.translate.notification

import kotlinx.coroutines.flow.combine
import mihon.entry.interactions.translate.work.EntryTranslateConditions
import mihon.entry.interactions.translate.work.EntryTranslateQueueRunner

/**
 * Shows that the translation queue was paused with chapters left, so it can be resumed or cleared from the
 * notifications after the worker's ongoing notification is gone, and takes the notice away once that no longer holds.
 */
internal class EntryTranslatePausedNotification(
    private val conditions: EntryTranslateConditions,
    private val runner: EntryTranslateQueueRunner,
    private val notifier: EntryTranslateNotifier,
) {
    suspend fun run() {
        // A queue already paused when the app starts is not announced again.
        var wasPaused: Boolean? = null
        combine(conditions.paused, runner.pending, ::Pair).collect { (paused, pending) ->
            if (!paused || !pending) {
                notifier.dismissPaused()
            } else if (wasPaused == false) {
                notifier.showPaused()
            }
            wasPaused = paused
        }
    }
}
