package eu.kanade.presentation.entry.translation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import mihon.entry.interactions.translate.EntryTranslateStatus
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The 文A mark a chapter's download indicator carries when the chapter is translated or on its way to it. */
@Composable
fun ChapterTranslationBadge(
    status: EntryTranslateStatus,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val tint = when (status) {
        EntryTranslateStatus.Translated, is EntryTranslateStatus.Translating -> colors.primary
        EntryTranslateStatus.WaitingForDownload, EntryTranslateStatus.Queued -> colors.onSurfaceVariant
        is EntryTranslateStatus.Failed -> colors.error
    }
    val description = when (status) {
        EntryTranslateStatus.Translated -> stringResource(MR.strings.chapter_translation_translated)
        EntryTranslateStatus.WaitingForDownload -> stringResource(MR.strings.chapter_translation_waiting)
        EntryTranslateStatus.Queued -> stringResource(MR.strings.chapter_translation_queued)
        is EntryTranslateStatus.Translating -> stringResource(
            MR.strings.chapter_translation_translating,
            status.progress.done,
            status.progress.total,
        )
        is EntryTranslateStatus.Failed -> stringResource(MR.strings.chapter_translation_failed)
    }
    Box(
        modifier = modifier
            .size(BadgeSize)
            .background(colors.surface, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (status is EntryTranslateStatus.Translating) {
            val progress = status.progress
            if (progress.total > 0) {
                CircularProgressIndicator(
                    progress = { progress.done.toFloat() / progress.total },
                    modifier = Modifier.size(BadgeSize),
                    color = tint,
                    strokeWidth = RingWidth,
                    trackColor = Color.Transparent,
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(BadgeSize),
                    color = tint,
                    strokeWidth = RingWidth,
                    trackColor = Color.Transparent,
                )
            }
        }
        Icon(
            imageVector = Icons.Outlined.Translate,
            contentDescription = description,
            modifier = Modifier.size(IconSize),
            tint = tint,
        )
    }
}

private val BadgeSize = 16.dp
private val IconSize = 10.dp
private val RingWidth = 1.5.dp
