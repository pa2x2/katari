package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.ChevronLeft
import app.katari.icons.materialsymbols.rounded.ChevronRight
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.button.IconButton
import app.katari.presentation.kit.content.StatisticsMetrics
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun AllRangeYearNavigation(
    year: Int,
    previousEnabled: Boolean,
    nextEnabled: Boolean,
    onAllActivity: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            label = stringResource(MR.strings.statistics_all_activity),
            onClick = onAllActivity,
            style = ButtonStyle.Text,
            icon = MaterialSymbols.Rounded.ChevronLeft,
        )
        IconButton(
            icon = MaterialSymbols.Rounded.ChevronLeft,
            contentDescription = stringResource(MR.strings.statistics_previous_year),
            onClick = onPrevious,
            enabled = previousEnabled,
        )
        Text(
            text = year.toString(),
            modifier = Modifier.weight(1f),
            role = TextRole.Label,
            textAlign = TextAlign.Center,
        )
        IconButton(
            icon = MaterialSymbols.Rounded.ChevronRight,
            contentDescription = stringResource(MR.strings.statistics_next_year),
            onClick = onNext,
            enabled = nextEnabled,
        )
    }
}

@Composable
internal fun AllRangeEmptyActivity() {
    Column(
        modifier = Modifier.fillMaxWidth().height(StatisticsMetrics.emptyActivityHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = stringResource(MR.strings.statistics_no_activity), role = TextRole.Label)
        Text(
            text = stringResource(MR.strings.statistics_all_activity_empty_hint),
            modifier = Modifier.padding(top = KatariTheme.spacing.s),
            role = TextRole.Supporting,
        )
    }
}
