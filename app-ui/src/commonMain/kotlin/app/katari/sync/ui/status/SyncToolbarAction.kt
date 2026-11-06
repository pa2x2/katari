package app.katari.sync.ui.status

import androidx.compose.runtime.Composable
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.CloudDone
import mihon.icons.materialsymbols.rounded.CloudOff
import mihon.icons.materialsymbols.rounded.CloudUpload
import mihon.icons.materialsymbols.rounded.Sync
import mihon.icons.materialsymbols.rounded.SyncProblem
import mihon.icons.materialsymbols.rounded.Wifi
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.theme.ContentTone

/**
 * Sync's icon in the Library toolbar (D028, D055), always there once the device is set up: turning arrows while it
 * syncs, a count while changes wait to be sent, a warning when it needs the person, a check mark when up to date. It
 * opens the Sync page. A standalone device shows none: there the Sync row in More is sync's only trace (D028).
 */
@Composable
fun rememberSyncToolbarAction(onClick: () -> Unit): AppBar.Action? {
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
        headline is SyncHeadline.Syncing || headline is SyncHeadline.Downloading ->
            AppBar.Action(title = title, icon = MaterialSymbols.Rounded.Sync, onClick = onClick, spinning = true)
        headline is SyncHeadline.Unsent -> AppBar.Action(
            title = title,
            icon = MaterialSymbols.Rounded.CloudUpload,
            onClick = onClick,
            badgeCount = headline.count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        )
        headline is SyncHeadline.FirstDownloadWaitsForWifi ->
            AppBar.Action(title = title, icon = MaterialSymbols.Rounded.Wifi, onClick = onClick)
        headline is SyncHeadline.Offline || headline is SyncHeadline.Unreachable ->
            AppBar.Action(title = title, icon = MaterialSymbols.Rounded.CloudOff, onClick = onClick)
        headline is SyncHeadline.HeldKinds ->
            AppBar.Action(title = title, icon = MaterialSymbols.Rounded.SyncProblem, onClick = onClick)
        else -> AppBar.Action(title = title, icon = MaterialSymbols.Rounded.CloudDone, onClick = onClick)
    }
}
