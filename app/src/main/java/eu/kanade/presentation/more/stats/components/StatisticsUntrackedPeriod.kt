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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import mihon.presentation.kit.content.StatisticsMetrics
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.floor

/** Diagonal lines on a global grid, so adjacent untracked buckets read as one continuous area. */
internal fun DrawScope.drawUntrackedHatch(
    left: Float,
    right: Float,
    top: Float,
    bottom: Float,
    color: Color,
    stroke: Dp,
) {
    val height = bottom - top
    val step = StatisticsMetrics.hatchSpacing.toPx()
    clipRect(left, top, right, bottom) {
        var x = floor((left - height) / step) * step
        while (x < right) {
            drawLine(color, Offset(x, bottom), Offset(x + height, top), strokeWidth = stroke.toPx())
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
    val hatchColor = KatariTheme.colors.outline
    val stroke = KatariTheme.sizes.border
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onShowLifetime != null) Modifier.clickable(onClick = onShowLifetime) else Modifier)
            .padding(top = KatariTheme.spacing.m, bottom = KatariTheme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(
            Modifier
                .size(StatisticsMetrics.hatchSample)
                .border(stroke, KatariTheme.colors.outlineVariant, KatariTheme.shapes.extraSmall),
        ) {
            drawUntrackedHatch(0f, size.width, 0f, size.height, hatchColor, stroke)
        }
        Spacer(Modifier.width(KatariTheme.spacing.s))
        Text(
            text = stringResource(MR.strings.statistics_not_tracked_yet),
            modifier = Modifier.weight(1f),
            role = TextRole.Supporting,
        )
        Text(
            text = stringResource(MR.strings.statistics_tracking_since, trackingStartDate.format(formatter)),
            role = TextRole.Supporting,
            tone = if (onShowLifetime != null) ContentTone.Primary else ContentTone.Secondary,
        )
    }
}
