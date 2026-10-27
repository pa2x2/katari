package eu.kanade.presentation.entry

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.util.lang.toTimestampString
import mihon.app.di.appGraph
import mihon.entry.interactions.presentation.EntryHistorySubtitlePresentation
import mihon.entry.interactions.presentation.EntryPartialProgressPresentation
import mihon.entry.interactions.presentation.EntryTypePresentationFeature
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.time.format.DateTimeFormatter
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds
import eu.kanade.presentation.util.toDurationString as durationToString

typealias EntryTypePresentation = mihon.entry.interactions.presentation.EntryTypePresentation

/**
 * Resolves contributed type vocabulary. Generic resolution retains the requested type so missing projection evidence
 * remains available to validation while every presentation surface receives renderable neutral vocabulary.
 */
fun EntryType?.entryTypePresentation(feature: EntryTypePresentationFeature): EntryTypePresentation {
    return feature.presentation(this).presentation
}

@Composable
@ReadOnlyComposable
fun EntryType?.entryTypePresentation(): EntryTypePresentation {
    return entryTypePresentation(LocalContext.current.appGraph.entryTypePresentationFeature)
}

fun Iterable<EntryType>.selectionEntryTypePresentation(feature: EntryTypePresentationFeature): EntryTypePresentation {
    return toSet().singleOrNull().entryTypePresentation(feature)
}

@Composable
@ReadOnlyComposable
fun Iterable<EntryType>.selectionEntryTypePresentation(): EntryTypePresentation {
    return selectionEntryTypePresentation(LocalContext.current.appGraph.entryTypePresentationFeature)
}

@Composable
fun EntryType.EntryTypeIcon(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tone: ContentTone = ContentTone.Inherit,
) {
    Icon(
        imageVector = entryTypePresentation().badgeIcon,
        contentDescription = contentDescription,
        modifier = modifier,
        tone = tone,
    )
}

/** The entry type's icon at text size, receding so it labels a row without competing with its title. */
@Composable
fun EntryType.InlineEntryTypeIndicator(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    EntryTypeIcon(
        contentDescription = contentDescription,
        modifier = modifier.size(KatariTheme.sizes.iconSmall),
        tone = ContentTone.Disabled,
    )
}

/** [childGroupTitle], such as a volume, leads the line when the child belongs to a group. */
@Composable
fun EntryType.historySubtitle(
    childName: String,
    childNumber: Double,
    consumedAt: Date?,
    consumedDuration: Long,
    childGroupTitle: String?,
    timeFormatter: DateTimeFormatter,
): String {
    val subtitle = when (val presentation = entryTypePresentation().historySubtitle) {
        EntryHistorySubtitlePresentation.NameTimestampAndDuration -> {
            val context = LocalContext.current
            buildString {
                append(childName)
                consumedAt?.toTimestampString(timeFormatter)?.takeIf(String::isNotBlank)?.let {
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
                    consumedAt?.toTimestampString(timeFormatter) ?: "",
                )
            } else {
                consumedAt?.toTimestampString(timeFormatter) ?: ""
            }
        }
        EntryHistorySubtitlePresentation.NameAndTimestamp -> buildString {
            append(childName)
            consumedAt?.toTimestampString(timeFormatter)?.takeIf(String::isNotBlank)?.let {
                append(" • ")
                append(it)
            }
        }
    }
    return childGroupTitle?.let { "$it · $subtitle" } ?: subtitle
}

@Composable
fun EntryType.partialProgressLabel(position: Long): String? {
    if (position <= 0L) return null

    return when (val presentation = entryTypePresentation().partialProgress) {
        is EntryPartialProgressPresentation.NumberedPosition -> stringResource(presentation.label, position + 1)
        is EntryPartialProgressPresentation.Fixed -> stringResource(presentation.label)
    }
}
