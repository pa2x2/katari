package eu.kanade.presentation.more.stats.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

internal object StatisticsTrendChartPagerIsolation : NestedScrollConnection {
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        return if (source == NestedScrollSource.UserInput) Offset(available.x, 0f) else Offset.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        return Velocity(available.x, 0f)
    }
}
