package eu.kanade.presentation.library.update.report.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.library.update.sourcePauseEndText
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.menu.DropdownMenu
import mihon.presentation.kit.menu.DropdownMenuItem
import tachiyomi.domain.library.update.model.SourceUpdatePause
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * Pauses a source for a chosen time. While the source is paused the chip shows until when, and tapping it resumes the
 * source.
 *
 * @param pause the source's pause in effect now, or null when it isn't paused.
 * @param onPause receives when the pause ends, in epoch milliseconds, or null to pause until the source is resumed.
 */
@Composable
internal fun SourcePauseChip(
    pause: SourceUpdatePause?,
    onPause: (Long?) -> Unit,
    onResume: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }
    val until = pause?.until
    Box {
        FilterChip(
            label = when {
                pause == null -> stringResource(MR.strings.library_update_report_pause_source)
                until == null -> stringResource(MR.strings.library_update_source_paused)
                else -> stringResource(
                    MR.strings.library_update_source_paused_until_short,
                    sourcePauseEndText(until),
                )
            },
            selected = pause != null,
            onClick = { if (pause != null) onResume() else menuOpen = true },
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            PausePreset.entries.forEach { preset ->
                DropdownMenuItem(
                    label = stringResource(preset.labelRes),
                    onClick = {
                        menuOpen = false
                        onPause(preset.length?.let { Clock.System.now().plus(it).toEpochMilliseconds() })
                    },
                )
            }
            DropdownMenuItem(
                label = stringResource(MR.strings.library_update_pause_pick),
                onClick = {
                    menuOpen = false
                    pickingEnd = true
                },
            )
        }
    }
    if (pickingEnd) {
        SourcePauseUntilDialog(
            onConfirm = {
                pickingEnd = false
                onPause(it)
            },
            onDismissRequest = { pickingEnd = false },
        )
    }
}

/** @param length null for a pause that lasts until the source is resumed. */
private enum class PausePreset(val length: Duration?, val labelRes: StringResource) {
    DAY(1.days, MR.strings.library_update_pause_day),
    THREE_DAYS(3.days, MR.strings.library_update_pause_three_days),
    WEEK(7.days, MR.strings.library_update_pause_week),
    UNTIL_RESUMED(null, MR.strings.library_update_pause_until_resumed),
}
