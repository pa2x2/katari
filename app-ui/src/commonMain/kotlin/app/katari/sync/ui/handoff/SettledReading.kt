package app.katari.sync.ui.handoff

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Clock

/** When reading or watching each entry last settled, so its screen can tell it was read while it was covered. */
@Inject
@SingleIn(AppScope::class)
class SettledReading {
    private val settledAt = MutableStateFlow<Map<Long, Long>>(emptyMap())

    /** Entry ids to when reading them last settled, in epoch milliseconds. */
    val lastSettled: StateFlow<Map<Long, Long>> = settledAt

    fun record(entryId: Long) {
        val now = Clock.System.now().toEpochMilliseconds()
        settledAt.update { it + (entryId to now) }
    }
}
