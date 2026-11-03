package eu.kanade.presentation.util

import android.text.format.DateUtils

internal actual fun relativeTimeSpan(epochMillis: Long, now: Long): String =
    DateUtils.getRelativeTimeSpanString(epochMillis, now, DateUtils.MINUTE_IN_MILLIS).toString()
