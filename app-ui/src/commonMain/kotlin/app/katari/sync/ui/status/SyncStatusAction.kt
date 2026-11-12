package app.katari.sync.ui.status

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import app.katari.icons.custommaterialsymbols.CustomMaterialSymbols
import app.katari.icons.custommaterialsymbols.rounded.CloudSyncArrows
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Cloud
import app.katari.icons.materialsymbols.rounded.CloudDone
import app.katari.icons.materialsymbols.rounded.CloudOff
import app.katari.icons.materialsymbols.rounded.CloudUpload
import app.katari.icons.materialsymbols.rounded.SyncDisabled
import app.katari.icons.materialsymbols.rounded.SyncProblem
import app.katari.platform.presentation.platformPresentation
import app.katari.presentation.kit.content.Spin
import app.katari.presentation.kit.navigation.AppBar
import app.katari.presentation.kit.theme.ContentTone
import app.katari.sync.ui.SyncUiGraph
import app.katari.ui.di.LocalAppUiGraph
import kotlinx.coroutines.launch
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Sync's status icon (D028, D055), in an app bar or at the foot of the side dock wherever the home destinations place it
 * ([eu.kanade.tachiyomi.ui.home.navigation.rememberBarSyncAction]): a cloud whose arrows turn while it syncs, so it
 * doesn't read as library update's refresh beside it; a count while changes wait to be sent, a warning when it needs the
 * person, a check mark when up to date. It opens the Sync page, and a long press syncs at once, as the page's Sync now
 * does, wherever the icon shows. A standalone device shows none: there the Sync row in More is sync's only trace (D028).
 */
@Composable
fun rememberSyncStatusAction(graph: SyncUiGraph = LocalAppUiGraph.current, onClick: () -> Unit): AppBar.Action? {
    val status = rememberSyncStatus(graph)
    val headline = rememberSyncHeadline() ?: return null
    if (headline == SyncHeadline.NotSetUp) return null
    val title = syncHeadlineText(headline)
    val scope = rememberCoroutineScope()
    val current by rememberUpdatedState(status)
    // Kept while syncing is on, so the run a press starts doesn't take the long press from under the finger; a press
    // while a run can't start, such as during one, does nothing, as the page's Sync now is disabled then
    val syncNow = AppBar.LongPress(stringResource(MR.strings.sync_action_sync_now)) {
        if (current?.canSyncNow == true) scope.launch { graph.syncAccount.syncNow() }
    }
    return statusAction(headline, title, onClick).copy(longPress = syncNow.takeIf { status?.isOn == true })
}

private fun statusAction(headline: SyncHeadline, title: String, onClick: () -> Unit): AppBar.Action =
    when {
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
        headline is SyncHeadline.Unfinished -> AppBar.Action(
            title = title,
            icon = MaterialSymbols.Rounded.CloudUpload,
            onClick = onClick,
            badgeCount = headline.unsent.coerceAtMost(Int.MAX_VALUE.toLong()).toInt().takeIf { it > 0 },
        )
        headline is SyncHeadline.FirstDownloadWaitsForWifi || headline is SyncHeadline.ChangesWaitForWifi ->
            AppBar.Action(title = title, icon = platformPresentation.unmeteredNetworkIcon, onClick = onClick)
        headline is SyncHeadline.Paused ->
            AppBar.Action(title = title, icon = MaterialSymbols.Rounded.SyncDisabled, onClick = onClick)
        headline is SyncHeadline.Offline || headline is SyncHeadline.Unreachable ->
            AppBar.Action(title = title, icon = MaterialSymbols.Rounded.CloudOff, onClick = onClick)
        else -> AppBar.Action(title = title, icon = MaterialSymbols.Rounded.CloudDone, onClick = onClick)
    }
