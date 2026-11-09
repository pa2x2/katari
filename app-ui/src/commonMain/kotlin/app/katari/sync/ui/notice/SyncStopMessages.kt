package app.katari.sync.ui.notice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import app.katari.presentation.kit.feedback.LocalAppMessages
import app.katari.presentation.kit.feedback.MessageAction
import app.katari.sync.ui.SyncUiGraph
import app.katari.sync.ui.status.SyncHeadline
import app.katari.sync.ui.status.syncHeadlineText
import app.katari.ui.di.LocalAppUiGraph
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Tells each stop of sync that happens while the app is in front ([SyncNotices.stoppedInFront]) once, as a message on
 * the screen the person is on, worded as sync's status says it, with Open calling [onOpen] to show the Sync page.
 */
@Composable
fun SyncStopMessages(onOpen: () -> Unit, graph: SyncUiGraph = LocalAppUiGraph.current) {
    val messages = LocalAppMessages.current
    val currentOnOpen by rememberUpdatedState(onOpen)
    var stop by remember { mutableStateOf<SyncHeadline?>(null) }
    LaunchedEffect(graph) { graph.syncNotices.stoppedInFront.collect { stop = it } }
    val told = stop ?: return
    val text = syncHeadlineText(told)
    val open = stringResource(MR.strings.action_open)
    LaunchedEffect(told) {
        messages.show(text, long = true, action = MessageAction(open) { currentOnOpen() })
        stop = null
    }
}
