package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import eu.kanade.presentation.more.stats.data.StatsActivityWindow
import eu.kanade.presentation.more.stats.data.StatsRange
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.ChevronLeft
import mihon.icons.materialsymbols.rounded.ChevronRight
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Moves a finite window by its own length; Today appears next to the dates once an older window is shown. */
@Composable
internal fun StatisticsWindowNavigation(
    window: StatsActivityWindow,
    olderEnabled: Boolean,
    newerEnabled: Boolean,
    onOlder: () -> Unit,
    onNewer: () -> Unit,
    onToday: () -> Unit,
) {
    val finite = window.range != StatsRange.ALL
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (finite) {
            IconButton(
                icon = MaterialSymbols.Rounded.ChevronLeft,
                contentDescription = stringResource(MR.strings.statistics_older_activity),
                onClick = onOlder,
                enabled = olderEnabled,
            )
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = formatStatisticsWindow(window), role = TextRole.Label, textAlign = TextAlign.Center)
            if (finite && !window.isLatest) {
                Button(label = stringResource(MR.strings.statistics_today), onClick = onToday, style = ButtonStyle.Text)
            }
        }
        if (finite) {
            IconButton(
                icon = MaterialSymbols.Rounded.ChevronRight,
                contentDescription = stringResource(MR.strings.statistics_newer_activity),
                onClick = onNewer,
                enabled = newerEnabled,
            )
        }
    }
}
