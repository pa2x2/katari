package app.katari.sync.ui.status

import androidx.compose.runtime.Composable
import app.katari.icons.custommaterialsymbols.CustomMaterialSymbols
import app.katari.icons.custommaterialsymbols.rounded.CloudSyncArrows
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Cloud
import app.katari.icons.materialsymbols.rounded.CloudDone
import app.katari.icons.materialsymbols.rounded.CloudOff
import app.katari.icons.materialsymbols.rounded.CloudUpload
import app.katari.icons.materialsymbols.rounded.SyncDisabled
import app.katari.icons.materialsymbols.rounded.SyncProblem
import app.katari.icons.materialsymbols.rounded.Wifi
import app.katari.presentation.kit.content.Spin
import app.katari.presentation.kit.navigation.AppBar
import app.katari.presentation.kit.theme.ContentTone

/**
 * Sync's status icon (D028, D055), in an app bar or at the foot of the side dock wherever the home destinations place it
 * ([eu.kanade.tachiyomi.ui.home.navigation.rememberBarSyncAction]): a cloud whose arrows turn while it syncs, so it
 * doesn't read as library update's refresh beside it; a count while changes wait to be sent, a warning when it needs the
 * person, a check mark when up to date. It opens the Sync page. A standalone device shows none: there the Sync row in
 * More is sync's only trace (D028).
 */
@Composable
fun rememberSyncStatusAction(onClick: () -> Unit): AppBar.Action? {
    val headline = rememberSyncHeadline() ?: return null
    if (headline == SyncHeadline.NotSetUp) return null
    val title = syncHeadlineText(headline)
    return when {
        headline.needsAttention -> AppBar.Action(
            title = title,
            icon = MaterialSymbols.Rounded.SyncProblem,
            onClick = onClick,
            tone = ContentTone.Warning,
        )
        headline is SyncHeadline.Syncing || headline is SyncHeadline.Downloading ||
            headline is SyncHeadline.CatchingUp || headline is SyncHeadline.Step ->
            AppBar.Action(
                title = title,
                icon = MaterialSymbols.Rounded.Cloud,
                onClick = onClick,
                spin = Spin.CounterClockwise,
                arrows = CustomMaterialSymbols.Rounded.CloudSyncArrows,
            )
        headline is SyncHeadline.Unsent -> AppBar.Action(
            title = title,
            icon = MaterialSymbols.Rounded.CloudUpload,
            onClick = onClick,
            badgeCount = headline.count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        )
        headline is SyncHeadline.FirstDownloadWaitsForWifi || headline is SyncHeadline.ChangesWaitForWifi ->
            AppBar.Action(title = title, icon = MaterialSymbols.Rounded.Wifi, onClick = onClick)
        headline is SyncHeadline.Paused ->
            AppBar.Action(title = title, icon = MaterialSymbols.Rounded.SyncDisabled, onClick = onClick)
        headline is SyncHeadline.Offline || headline is SyncHeadline.Unreachable ->
            AppBar.Action(title = title, icon = MaterialSymbols.Rounded.CloudOff, onClick = onClick)
        else -> AppBar.Action(title = title, icon = MaterialSymbols.Rounded.CloudDone, onClick = onClick)
    }
}
