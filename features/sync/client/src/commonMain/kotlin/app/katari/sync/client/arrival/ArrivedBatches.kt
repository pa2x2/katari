package app.katari.sync.client.arrival

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** The syncs that changed settings here from other devices, by their batch, told once they are committed. */
@Inject
@SingleIn(AppScope::class)
class ArrivedBatches {
    private val told = MutableSharedFlow<Long>(extraBufferCapacity = 8)

    val batches: SharedFlow<Long> = told.asSharedFlow()

    internal fun tell(batch: Long) {
        told.tryEmit(batch)
    }
}
