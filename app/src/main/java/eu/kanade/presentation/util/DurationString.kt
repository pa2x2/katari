package eu.kanade.presentation.util

import android.content.Context
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource
import kotlin.time.Duration

fun Duration.toDurationString(context: Context, fallback: String): String {
    return toComponents { days, hours, minutes, seconds, _ ->
        buildList(4) {
            if (days != 0L) add(context.stringResource(MR.strings.day_short, days))
            if (hours != 0) add(context.stringResource(MR.strings.hour_short, hours))
            if (minutes != 0 && (days == 0L || hours == 0)) {
                add(
                    context.stringResource(MR.strings.minute_short, minutes),
                )
            }
            if (seconds != 0 && days == 0L && hours == 0) add(context.stringResource(MR.strings.seconds_short, seconds))
        }.joinToString(" ").ifBlank { fallback }
    }
}
