package eu.kanade.presentation.entry.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import tachiyomi.domain.entry.model.EntryStatus
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

internal data class EntryStatusPresentation(
    val icon: ImageVector,
    val label: String,
)

@Composable
internal fun EntryStatus.presentation(): EntryStatusPresentation = when (this) {
    EntryStatus.ONGOING -> EntryStatusPresentation(Icons.Outlined.Schedule, stringResource(MR.strings.ongoing))
    EntryStatus.COMPLETED -> EntryStatusPresentation(Icons.Outlined.DoneAll, stringResource(MR.strings.completed))
    EntryStatus.LICENSED -> EntryStatusPresentation(Icons.Outlined.Verified, stringResource(MR.strings.licensed))
    EntryStatus.PUBLISHING_FINISHED -> {
        EntryStatusPresentation(Icons.Outlined.Done, stringResource(MR.strings.publishing_finished))
    }
    EntryStatus.CANCELLED -> EntryStatusPresentation(Icons.Outlined.Close, stringResource(MR.strings.cancelled))
    EntryStatus.ON_HIATUS -> EntryStatusPresentation(Icons.Outlined.Pause, stringResource(MR.strings.on_hiatus))
    else -> EntryStatusPresentation(
        Icons.AutoMirrored.Outlined.HelpOutline,
        stringResource(MR.strings.unknown),
    )
}
