package eu.kanade.presentation.entry.activity

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import tachiyomi.domain.statistics.entry.EntryActivitySummary
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.text.SimpleDateFormat
import java.util.Date

/** One-line digest of a title's activity under its action row; opens [EntryActivitySheet]. */
@Composable
fun EntryActivityLine(
    summary: EntryActivitySummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatDuration = rememberStatisticsDurationFormatter()
    val locale = LocalConfiguration.current.locales[0]
    val started = remember(summary.startedAtEpochMillis, locale) {
        summary.startedAtEpochMillis?.let {
            SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "MMMyyyy"), locale).format(Date(it))
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Timer,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = listOfNotNull(
                formatDuration(summary.totalDurationMillis),
                started?.let { stringResource(MR.strings.entry_activity_since, it) },
            ).joinToString(" · "),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = stringResource(MR.strings.entry_activity_title),
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
