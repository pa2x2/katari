package eu.kanade.presentation.entry

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.util.lang.toTimestampString
import mihon.entry.interactions.EntryHistorySubtitlePresentation
import mihon.entry.interactions.EntryPartialProgressPresentation
import mihon.entry.interactions.EntryTypePresentationFeature
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds
import eu.kanade.presentation.util.toDurationString as durationToString

typealias EntryTypePresentation = mihon.entry.interactions.EntryTypePresentation

object EntryTypeIconDefaults {
    val InlineSize = 16.dp
    val CoverOverlaySize = 16.dp
}

/**
 * Resolves contributed type vocabulary. Generic resolution retains the requested type so missing projection evidence
 * remains available to validation while every presentation surface receives renderable neutral vocabulary.
 */
fun EntryType?.entryTypePresentation(
    feature: EntryTypePresentationFeature = Injekt.get(),
): EntryTypePresentation {
    return feature.presentation(this).presentation
}

fun Iterable<EntryType>.selectionEntryTypePresentation(
    feature: EntryTypePresentationFeature = Injekt.get(),
): EntryTypePresentation {
    return toSet().singleOrNull().entryTypePresentation(feature)
}

@Composable
fun EntryType.EntryTypeIcon(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = LocalContentColor.current,
) {
    Icon(
        imageVector = entryTypePresentation().badgeIcon,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
    )
}

@Composable
fun EntryType.InlineEntryTypeIndicator(
    modifier: Modifier = Modifier,
    size: Dp = EntryTypeIconDefaults.InlineSize,
    contentDescription: String? = null,
    tint: Color = LocalContentColor.current.copy(alpha = DISABLED_ALPHA),
) {
    EntryTypeIcon(
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint,
    )
}

@Composable
fun EntryType.coverTypeIndicatorOverlay(): (@Composable BoxScope.() -> Unit)? {
    val icon = entryTypePresentation().coverOverlayIcon ?: return null
    return {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .padding(4.dp)
                .size(EntryTypeIconDefaults.CoverOverlaySize)
                .align(Alignment.TopStart),
            tint = Color.White,
        )
    }
}

@Composable
fun EntryType.historySubtitle(
    childName: String,
    childNumber: Double,
    consumedAt: Date?,
    consumedDuration: Long,
): String {
    return when (val presentation = entryTypePresentation().historySubtitle) {
        EntryHistorySubtitlePresentation.NameTimestampAndDuration -> {
            val context = LocalContext.current
            buildString {
                append(childName)
                consumedAt?.toTimestampString()?.takeIf(String::isNotBlank)?.let {
                    append(" • ")
                    append(it)
                }
                if (consumedDuration > 0L) {
                    append(" • ")
                    append(
                        consumedDuration.milliseconds.durationToString(
                            context = context,
                            fallback = stringResource(MR.strings.not_applicable),
                        ),
                    )
                }
            }
        }
        is EntryHistorySubtitlePresentation.NumberAndTimestamp -> {
            if (childNumber > -1) {
                stringResource(
                    presentation.label,
                    formatChapterNumber(childNumber),
                    consumedAt?.toTimestampString() ?: "",
                )
            } else {
                consumedAt?.toTimestampString() ?: ""
            }
        }
        EntryHistorySubtitlePresentation.NameAndTimestamp -> buildString {
            append(childName)
            consumedAt?.toTimestampString()?.takeIf(String::isNotBlank)?.let {
                append(" • ")
                append(it)
            }
        }
    }
}

@Composable
fun EntryType.partialProgressLabel(position: Long): String? {
    if (position <= 0L) return null

    return when (val presentation = entryTypePresentation().partialProgress) {
        is EntryPartialProgressPresentation.NumberedPosition -> stringResource(presentation.label, position + 1)
        is EntryPartialProgressPresentation.Fixed -> stringResource(presentation.label)
    }
}
