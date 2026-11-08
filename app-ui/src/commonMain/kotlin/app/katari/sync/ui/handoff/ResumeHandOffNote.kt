package app.katari.sync.ui.handoff

import androidx.compose.runtime.Composable
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.CloudOff
import app.katari.icons.materialsymbols.rounded.Devices
import app.katari.presentation.kit.chip.FloatingNote
import eu.kanade.presentation.util.relativeTimeSpanString
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** [note] beside Resume, such as "From Fedora desktop · 2 minutes ago". */
@Composable
fun ResumeHandOffNote(note: HandOffNote) {
    when (note) {
        HandOffNote.Checking -> FloatingNote(stringResource(MR.strings.sync_handoff_checking))
        is HandOffNote.From -> FloatingNote(
            text = stringResource(
                MR.strings.sync_handoff_from,
                note.deviceName ?: stringResource(MR.strings.sync_unknown_device),
                relativeTimeSpanString(note.atMillis),
            ),
            icon = MaterialSymbols.Rounded.Devices,
        )
        HandOffNote.Own ->
            FloatingNote(stringResource(MR.strings.sync_handoff_own), icon = MaterialSymbols.Rounded.CloudOff)
    }
}
