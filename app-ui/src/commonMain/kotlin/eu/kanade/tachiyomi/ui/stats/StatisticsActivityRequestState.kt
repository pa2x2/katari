package eu.kanade.tachiyomi.ui.stats

import eu.kanade.presentation.more.stats.ActivityState
import eu.kanade.presentation.more.stats.data.StatsActivity
import eu.kanade.presentation.more.stats.data.StatsActivityWindow
import eu.kanade.presentation.more.stats.data.StatsRange

internal data class StatisticsActivityLoadRequest(
    val window: StatsActivityWindow,
    val reloadToken: Long,
)

internal sealed interface StatisticsActivityLoadEvent {
    val request: StatisticsActivityLoadRequest

    data class Loading(override val request: StatisticsActivityLoadRequest) : StatisticsActivityLoadEvent

    data class Loaded(
        override val request: StatisticsActivityLoadRequest,
        val data: StatsActivity,
    ) : StatisticsActivityLoadEvent

    data class Failed(override val request: StatisticsActivityLoadRequest) : StatisticsActivityLoadEvent
}

internal fun reduceStatisticsActivityRequest(
    previous: ActivityState?,
    event: StatisticsActivityLoadEvent,
): ActivityState = when (event) {
    is StatisticsActivityLoadEvent.Loading -> when {
        previous is ActivityState.Available &&
            previous.failedTarget != null &&
            event.request.window == previous.data.window -> previous
        previous is ActivityState.Available -> previous.copy(
            loadingTarget = event.request.window,
            failedTarget = null,
        )
        else -> ActivityState.Loading(event.request.window)
    }
    is StatisticsActivityLoadEvent.Loaded -> when {
        previous is ActivityState.Available &&
            previous.failedTarget != null &&
            event.data.window == previous.data.window -> previous.copy(
            data = event.data,
            loadingTarget = null,
        )
        else -> ActivityState.Available(event.data)
    }
    is StatisticsActivityLoadEvent.Failed -> when (previous) {
        is ActivityState.Available -> previous.copy(
            loadingTarget = null,
            failedTarget = event.request.window,
        )
        else -> ActivityState.Failed(event.request.window)
    }
}

internal val ActivityState.displayedRange: StatsRange
    get() = when (this) {
        is ActivityState.Loading -> target.range
        is ActivityState.Available -> data.window.range
        is ActivityState.Failed -> target.range
    }
