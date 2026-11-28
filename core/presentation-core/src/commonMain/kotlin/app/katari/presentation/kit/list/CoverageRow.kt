package app.katari.presentation.kit.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.roundedfilled.Cancel
import app.katari.icons.materialsymbols.roundedfilled.CheckCircle
import app.katari.icons.materialsymbols.roundedfilled.DoNotDisturbOn
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.progress.LinearProgressIndicator
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme

/**
 * How much of one kind of data came over from elsewhere, as a row of a report, such as a row of what a backup from
 * another app brings over: a mark for all, part or none of it, the [title], an [amount] line saying how much and a bar
 * of [carried] out of [total]. A row with [details], such as the lists of what came over and what didn't, opens them
 * under it while [expanded].
 */
@Composable
fun CoverageRow(
    title: String,
    amount: String,
    carried: Int,
    total: Int,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    onClick: (() -> Unit)? = null,
    details: (@Composable () -> Unit)? = null,
) {
    val (mark, tone) = when {
        carried >= total -> MaterialSymbols.RoundedFilled.CheckCircle to ContentTone.Success
        carried == 0 -> MaterialSymbols.RoundedFilled.Cancel to ContentTone.Error
        else -> MaterialSymbols.RoundedFilled.DoNotDisturbOn to ContentTone.Warning
    }
    Column(modifier) {
        SettingsRow(
            title = title,
            subtitle = amount,
            leading = { Icon(mark, contentDescription = null, tone = tone) },
            expanded = expanded.takeIf { details != null },
            onClick = onClick.takeIf { details != null },
            below = {
                LinearProgressIndicator(
                    progress = { if (total == 0) 1f else carried.toFloat() / total },
                    modifier = Modifier.fillMaxWidth().padding(top = KatariTheme.spacing.s),
                )
            },
        )
        if (expanded && details != null) details()
    }
}
