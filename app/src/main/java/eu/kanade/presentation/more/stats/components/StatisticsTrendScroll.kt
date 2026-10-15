package eu.kanade.presentation.more.stats.components

import java.time.LocalDate
import kotlin.math.roundToInt

/*
 * Scrolling the activity chart: a drag moves the plot freely, and once it settles the chart shifts its window by
 * whole buckets and keeps the leftover offset, so the bars stay under the finger while the new window loads.
 */

internal data class SettledTrendOffset(
    val bucketShift: Int,
    val residualOffsetPx: Float,
)

internal fun settleTrendOffset(
    offsetPx: Float,
    pointSpacingPx: Float,
    olderBucketCount: Int,
    newerBucketCount: Int,
): SettledTrendOffset {
    if (pointSpacingPx <= 0f) return SettledTrendOffset(0, offsetPx)
    val bucketShift = (offsetPx / pointSpacingPx).roundToInt()
        .coerceIn(-newerBucketCount, olderBucketCount)
    return SettledTrendOffset(
        bucketShift = bucketShift,
        residualOffsetPx = offsetPx - bucketShift * pointSpacingPx,
    )
}

/** A window shift requested from a settled drag, until the new window arrives and its offset is reconciled. */
internal data class PendingTrendNavigation(
    val sourceWindowStart: LocalDate?,
    val requestedBucketShift: Int,
    val restingOffsetBuckets: Float,
)
