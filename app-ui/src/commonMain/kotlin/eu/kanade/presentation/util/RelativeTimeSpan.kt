package eu.kanade.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

@Composable
@ReadOnlyComposable
fun relativeTimeSpanString(epochMillis: Long): String {
    val now = Clock.System.now().toEpochMilliseconds()
    return when {
        epochMillis <= 0L -> stringResource(MR.strings.relative_time_span_never)
        // Both ways: next runs are in the future, and a time recorded before the clock was set back is too.
        abs(now - epochMillis) < 1.minutes.inWholeMilliseconds -> stringResource(
            MR.strings.updates_last_update_info_just_now,
        )
        else -> relativeTimeSpan(epochMillis, now)
    }
}

/**
 * [epochMillis] relative to [now] in whole minutes, hours or days, such as "5 minutes ago", "in 3 hours" or
 * "yesterday", and as its date once it is a week or more away.
 */
internal expect fun relativeTimeSpan(epochMillis: Long, now: Long): String
