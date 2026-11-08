package eu.kanade.presentation.entry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.dialog.Dialog
import app.katari.presentation.kit.dialog.DialogAction
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.ui.di.LocalAppUiGraph
import eu.kanade.presentation.entry.updates.entryReleaseEstimateText
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.service.nextupdate.FetchInterval
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.WheelPickerDefaults
import tachiyomi.presentation.core.components.WheelTextPicker
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Instant

/** Explains when the entry is expected to update next; debug builds can also set a custom interval. */
@Composable
fun SetIntervalDialog(
    interval: Int,
    nextUpdate: Instant?,
    entryType: EntryType? = null,
    onDismissRequest: () -> Unit,
    onValueChanged: ((Int) -> Unit)? = null,
) {
    var selectedInterval by rememberSaveable { mutableIntStateOf(if (interval < 0) -interval else 0) }

    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.entry_updates_release_estimate),
        confirm = DialogAction(stringResource(MR.strings.action_save), onClick = {
            onValueChanged?.invoke(selectedInterval)
            onDismissRequest()
        }),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s)) {
            Text(entryReleaseEstimateText(interval, nextUpdate, entryType))

            if (onValueChanged != null && LocalAppUiGraph.current.entryUpdateIntervalAvailability.isOffered) {
                Text(stringResource(MR.strings.manga_interval_custom_amount))

                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    val size = DpSize(width = maxWidth / 2, height = WheelPickerDefaults.Size.height)
                    val items = (0..FetchInterval.MAX_INTERVAL)
                        .map {
                            if (it == 0) {
                                stringResource(MR.strings.label_default)
                            } else {
                                it.toString()
                            }
                        }

                    WheelTextPicker(
                        items = items,
                        size = size,
                        startIndex = selectedInterval,
                        onSelectionChanged = { selectedInterval = it },
                    )
                }
            }
        }
    }
}
