package mihon.entry.interactions.translate.work

import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import mihon.entry.interactions.translate.EntryTranslateWaiting

/**
 * One run of the translation queue in the background, as each platform's translation work starts it: it translates
 * until the queue is empty or paused. While the charging or Wi-Fi rule holds the queue, it waits; the chapter being
 * translated when the rules start holding it keeps its place and starts over once they let it go.
 */
class EntryTranslateRun internal constructor(
    private val runner: EntryTranslateQueueRunner,
    private val conditions: EntryTranslateConditions,
) {
    suspend fun run() = run(onPending = {}, onStatus = { _, _ -> })

    /**
     * @param onPending called each time chapters are pending, before they wait for the rules; Android's worker goes to
     * the foreground there.
     * @param onStatus gets the chapter being translated and what the queue waits for, while the run is under way.
     */
    internal suspend fun run(
        onPending: suspend () -> Unit,
        onStatus: (EntryTranslateQueueRunner.Active?, EntryTranslateWaiting?) -> Unit,
    ) {
        while (true) {
            if (conditions.paused.first() || !runner.hasPendingWork()) return
            onPending()
            if (runUntilHeld(onStatus)) return
        }
    }

    /**
     * Runs until the queue is idle or paused, which returns true, or until the rules hold it, which returns false.
     */
    private suspend fun runUntilHeld(
        onStatus: (EntryTranslateQueueRunner.Active?, EntryTranslateWaiting?) -> Unit,
    ): Boolean = coroutineScope {
        val status = launch {
            combine(runner.active, conditions.waiting, ::Pair).collect { (active, waiting) ->
                onStatus(active, waiting)
            }
        }
        // Chapters cancelled or a queue paused while it is held leave nothing to wait for.
        combine(conditions.waiting, runner.pending, conditions.paused) { waiting, pending, paused ->
            waiting == null || !pending || paused
        }.first { it }
        if (conditions.paused.first()) {
            status.cancel()
            return@coroutineScope true
        }
        val processing = async { runner.runUntilIdle() }
        val held = async {
            combine(conditions.waiting, conditions.paused) { waiting, paused -> waiting != null || paused }.first { it }
        }
        select {
            processing.onAwait {
                held.cancelAndJoin()
                true
            }
            held.onAwait {
                processing.cancelAndJoin()
                conditions.paused.first()
            }
        }.also { status.cancel() }
    }
}
