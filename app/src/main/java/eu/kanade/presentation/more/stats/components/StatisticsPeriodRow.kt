package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Period label used when the activity chart is hidden, keeping a way back to the latest window. */
@Composable
internal fun StatisticsPeriodRow(
    period: String,
    showToday: Boolean,
    onToday: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = period, modifier = Modifier.weight(1f), role = TextRole.Supporting)
        if (showToday) {
            Button(label = stringResource(MR.strings.statistics_today), onClick = onToday, style = ButtonStyle.Text)
        }
    }
}
