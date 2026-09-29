package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.floor

/** Diagonal lines on a global grid, so adjacent untracked buckets read as one continuous area. */
internal fun DrawScope.drawUntrackedHatch(left: Float, right: Float, top: Float, bottom: Float, color: Color) {
    val height = bottom - top
    val step = HATCH_SPACING.toPx()
    clipRect(left, top, right, bottom) {
        var x = floor((left - height) / step) * step
        while (x < right) {
            drawLine(color, Offset(x, bottom), Offset(x + height, top), strokeWidth = 1.dp.toPx())
            x += step
        }
    }
}

/**
 * Legend for hatched chart buckets: they predate [trackingStartDate], so their activity only exists in the
 * Lifetime total. [onShowLifetime] is null where the Lifetime card is already on the page.
 */
@Composable
internal fun StatisticsTrackingStartRow(trackingStartDate: LocalDate, onShowLifetime: (() -> Unit)?) {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    val hatchColor = MaterialTheme.colorScheme.outline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onShowLifetime != null) Modifier.clickable(onClick = onShowLifetime) else Modifier)
            .padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(
            Modifier
                .size(12.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraSmall),
        ) {
            drawUntrackedHatch(0f, size.width, 0f, size.height, hatchColor)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(MR.strings.statistics_not_tracked_yet),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(MR.strings.statistics_tracking_since, trackingStartDate.format(formatter)),
            style = MaterialTheme.typography.labelMedium,
            color = if (onShowLifetime != null) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

private val HATCH_SPACING = 6.dp
