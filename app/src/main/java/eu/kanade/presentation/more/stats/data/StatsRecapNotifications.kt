package eu.kanade.presentation.more.stats.data

import androidx.compose.runtime.Immutable

/** Which recaps announce themselves with a notification. */
@Immutable
data class StatsRecapNotifications(val monthly: Boolean = false, val yearly: Boolean = true)
