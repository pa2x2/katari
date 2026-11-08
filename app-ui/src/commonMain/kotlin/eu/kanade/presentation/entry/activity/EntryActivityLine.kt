package eu.kanade.presentation.entry.activity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.style.TextOverflow
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.automirroredrounded.KeyboardArrowRight
import app.katari.icons.materialsymbols.rounded.Timer
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import eu.kanade.domain.ui.bestDateTimePattern
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
    val locale = LocalLocale.current.platformLocale
    val started = remember(summary.startedAtEpochMillis, locale) {
        summary.startedAtEpochMillis?.let {
            SimpleDateFormat(bestDateTimePattern(locale, "MMMyyyy"), locale).format(Date(it))
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = KatariTheme.spacing.contentInset, vertical = KatariTheme.spacing.s),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = MaterialSymbols.Rounded.Timer,
            contentDescription = null,
            modifier = Modifier.size(KatariTheme.sizes.iconSmall),
            tone = ContentTone.Primary,
        )
        Text(
            text = listOfNotNull(
                formatDuration(summary.totalDurationMillis),
                started?.let { stringResource(MR.strings.entry_activity_since, it) },
            ).joinToString(" · "),
            modifier = Modifier.weight(1f),
            role = TextRole.Body,
            tone = ContentTone.Secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            imageVector = MaterialSymbols.AutoMirroredRounded.KeyboardArrowRight,
            contentDescription = stringResource(MR.strings.entry_activity_title),
            modifier = Modifier.size(KatariTheme.sizes.iconSmall),
            tone = ContentTone.Secondary,
        )
    }
}
