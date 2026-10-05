package eu.kanade.tachiyomi.ui.stats.recap

import android.content.Context
import tachiyomi.core.common.i18n.pluralStringResource

/** What was finished in the recap's period, worded by type; null when nothing was. */
fun StatisticsRecap.consumedCountsText(context: Context): String? {
    return consumedCounts
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" · ") { context.pluralStringResource(it.plural, it.count.toInt(), it.count.toInt()) }
}
