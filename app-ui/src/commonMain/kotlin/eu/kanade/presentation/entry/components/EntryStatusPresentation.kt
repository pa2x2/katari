package eu.kanade.presentation.entry.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.automirroredrounded.EventUpcoming
import app.katari.icons.materialsymbols.automirroredrounded.Help
import app.katari.icons.materialsymbols.rounded.Check
import app.katari.icons.materialsymbols.rounded.Close
import app.katari.icons.materialsymbols.rounded.DoneAll
import app.katari.icons.materialsymbols.rounded.Schedule
import app.katari.icons.materialsymbols.rounded.Verified
import app.katari.icons.materialsymbols.roundedfilled.Pause
import tachiyomi.domain.entry.model.EntryStatus
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

data class EntryStatusPresentation(
    val icon: ImageVector,
    val label: String,
)

@Composable
fun EntryStatus.presentation(): EntryStatusPresentation = when (this) {
    EntryStatus.ONGOING -> EntryStatusPresentation(
        MaterialSymbols.Rounded.Schedule,
        stringResource(MR.strings.ongoing),
    )
    EntryStatus.COMPLETED -> EntryStatusPresentation(
        MaterialSymbols.Rounded.DoneAll,
        stringResource(MR.strings.completed),
    )
    EntryStatus.LICENSED -> EntryStatusPresentation(
        MaterialSymbols.Rounded.Verified,
        stringResource(MR.strings.licensed),
    )
    EntryStatus.PUBLISHING_FINISHED -> {
        EntryStatusPresentation(MaterialSymbols.Rounded.Check, stringResource(MR.strings.publishing_finished))
    }
    EntryStatus.CANCELLED -> EntryStatusPresentation(
        MaterialSymbols.Rounded.Close,
        stringResource(MR.strings.cancelled),
    )
    EntryStatus.ON_HIATUS -> EntryStatusPresentation(
        MaterialSymbols.RoundedFilled.Pause,
        stringResource(MR.strings.on_hiatus),
    )
    EntryStatus.UPCOMING -> EntryStatusPresentation(
        MaterialSymbols.AutoMirroredRounded.EventUpcoming,
        stringResource(MR.strings.not_yet_released),
    )
    else -> EntryStatusPresentation(
        MaterialSymbols.AutoMirroredRounded.Help,
        stringResource(MR.strings.unknown),
    )
}
