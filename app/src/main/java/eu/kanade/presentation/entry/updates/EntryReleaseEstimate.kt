package eu.kanade.presentation.entry.updates

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.absoluteValue
import kotlin.time.Clock
import kotlin.time.Instant

/** Days until [nextUpdate], or null when no release is predicted. */
fun daysUntilRelease(nextUpdate: Instant?): Int? {
    return nextUpdate?.let { Clock.System.now().daysUntil(it, TimeZone.currentSystemDefault()).coerceAtLeast(0) }
}

/**
 * When the next release is predicted and how often the entry seems to release.
 *
 * @param interval days between releases; negative when the user set it.
 */
@Composable
@ReadOnlyComposable
fun entryReleaseEstimateText(interval: Int, nextUpdate: Instant?, entryType: EntryType?): String {
    val presentation = entryType.entryTypePresentation()
    val nextUpdateDays = daysUntilRelease(nextUpdate)
    return if (nextUpdateDays != null && interval >= 0) {
        stringResource(
            presentation.intervalExpectedUpdateLabel,
            pluralStringResource(MR.plurals.day, count = nextUpdateDays, nextUpdateDays),
            pluralStringResource(MR.plurals.day, count = interval.absoluteValue, interval.absoluteValue),
        )
    } else {
        stringResource(presentation.intervalExpectedUpdateNullLabel)
    }
}
