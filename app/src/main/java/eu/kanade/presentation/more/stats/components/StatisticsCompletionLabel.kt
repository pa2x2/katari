package eu.kanade.presentation.more.stats.components

import android.icu.text.ListFormatter
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Names a count of finished items by the units it adds up, such as "Chapters" or "Chapters and episodes". Only nouns
 * are used so the label stays one line in a summary tile whichever units are counted. Types with nothing finished don't
 * decide the label unless no type has any.
 */
@Composable
internal fun statisticsCompletionLabel(types: List<StatsType>, completionCountByType: Map<EntryType, Long>): String {
    val units = types.filter { (completionCountByType[it.type] ?: 0L) > 0L }
        .ifEmpty { types }
        .map { stringResource(it.consumedUnitName) }
        .distinct()
    val locale = LocalConfiguration.current.locales[0]
    return ListFormatter.getInstance(locale).format(units).replaceFirstChar { it.titlecase(locale) }
}
