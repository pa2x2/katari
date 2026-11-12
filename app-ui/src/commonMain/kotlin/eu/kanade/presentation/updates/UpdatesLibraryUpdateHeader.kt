package eu.kanade.presentation.updates

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.katari.core.platform.currentPlatformContext
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.content.TonedSpan
import app.katari.presentation.kit.content.TonedText
import app.katari.presentation.kit.feedback.StatusLine
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.TextRole
import app.katari.sync.client.libraryupdate.OtherDeviceLibraryUpdate
import eu.kanade.presentation.library.update.libraryUpdateTimeOn
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.data.library.label
import tachiyomi.domain.library.update.model.LibraryUpdateResultsSoFar
import tachiyomi.domain.library.update.model.LibraryUpdateState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/** What the top of Updates says about library updates. */
@Immutable
data class UpdatesLibraryUpdateStatus(
    /** This device's update while it runs or waits its turn, shown in place of the latest one. */
    val update: LibraryUpdateState?,
    /** What [update] found so far while it runs. */
    val resultsSoFar: LibraryUpdateResultsSoFar?,
    /** When this device's latest update ended. */
    val lastUpdated: Long,
    /** Entries the latest update failed to check, named so a failure is noticed without opening the report. */
    val failed: Int,
    /** Another device's update of the whole library that started after this device's, named in its place. */
    val otherDevice: OtherDeviceLibraryUpdate?,
    /** The sources library updates leave out, named so a pause isn't forgotten. */
    val pausedSources: List<String>,
    /** Whether an update of this device or another left a report to open. */
    val hasReport: Boolean,
)

internal fun LazyListScope.updatesLibraryUpdateItem(
    status: UpdatesLibraryUpdateStatus,
    onClick: (() -> Unit)?,
    onCancel: () -> Unit,
) {
    item(key = "updates-lastUpdated") {
        UpdatesLibraryUpdateHeader(
            status = status,
            onClick = onClick,
            onCancel = onCancel,
            modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
        )
    }
}

/**
 * How library updates stand: how far a running update got and what it found so far, or when the latest update ended
 * and how it went.
 *
 * @param onClick opens the latest update's report; null while there is none.
 * @param onCancel stops the running update, or takes a waiting one out of the queue.
 */
@Composable
internal fun UpdatesLibraryUpdateHeader(
    status: UpdatesLibraryUpdateStatus,
    onClick: (() -> Unit)?,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val update = status.update
    val progress = (update as? LibraryUpdateState.Running)?.progress
    StatusLine(
        onClick = onClick,
        modifier = modifier,
        progress = progress?.let { { if (it.total > 0) it.completed.toFloat() / it.total else 0f } },
        actionLabel = update?.let { stringResource(MR.strings.action_cancel) },
        onAction = onCancel,
    ) {
        when (update) {
            is LibraryUpdateState.Running -> RunningUpdateText(update, status.resultsSoFar)
            is LibraryUpdateState.Waiting -> Text(
                text = stringResource(MR.strings.library_update_waiting, update.runningProfileName),
                role = TextRole.Supporting,
            )
            null -> LatestUpdateText(status)
        }
        if (status.pausedSources.isNotEmpty()) {
            Text(text = pausedSourcesText(status.pausedSources), role = TextRole.Supporting)
        }
    }
}

@Composable
private fun RunningUpdateText(update: LibraryUpdateState.Running, results: LibraryUpdateResultsSoFar?) {
    val context = currentPlatformContext()
    val label = remember(update.progress, context) { update.progress.label(context) }
    TonedText(
        spans = listOfNotNull(
            TonedSpan(label),
            results?.newChapters?.takeIf { it > 0 }?.let {
                TonedSpan(pluralStringResource(MR.plurals.library_updates_new_chapters, it, it), keepWhole = true)
            },
            results?.failed?.takeIf { it > 0 }?.let {
                TonedSpan(
                    text = pluralStringResource(MR.plurals.library_updates_failed, it, it),
                    tone = ContentTone.Error,
                    keepWhole = true,
                )
            },
        ),
        role = TextRole.Supporting,
        separator = " · ",
    )
}

@Composable
private fun LatestUpdateText(status: UpdatesLibraryUpdateStatus) {
    val otherDevice = status.otherDevice
    val updatedAt = if (otherDevice != null) {
        val run = otherDevice.summary.run
        libraryUpdateTimeOn(relativeTimeSpanString(run.finishedAt ?: run.startedAt), otherDevice.deviceName)
    } else {
        relativeTimeSpanString(status.lastUpdated)
    }
    val failed = otherDevice?.summary?.failed ?: status.failed
    TonedText(
        spans = listOfNotNull(
            TonedSpan(stringResource(MR.strings.updates_last_update_info, updatedAt)),
            TonedSpan(
                text = pluralStringResource(MR.plurals.library_updates_failed, failed, failed),
                tone = ContentTone.Error,
                keepWhole = true,
            ).takeIf { failed > 0 },
        ),
        role = TextRole.Supporting,
        separator = " · ",
    )
}

@Composable
private fun pausedSourcesText(names: List<String>): String {
    return when (names.size) {
        1 -> stringResource(MR.strings.updates_paused_source, names[0])
        2 -> stringResource(MR.strings.updates_paused_two_sources, names[0], names[1])
        else -> pluralStringResource(MR.plurals.updates_paused_sources, names.size, names.size)
    }
}
