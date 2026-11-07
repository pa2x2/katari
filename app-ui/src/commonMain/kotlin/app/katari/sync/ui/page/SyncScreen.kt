package app.katari.sync.ui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.katari.sync.client.status.SyncStatus
import app.katari.sync.ui.firstsync.FirstSyncScreen
import app.katari.sync.ui.link.LinkDeviceDialog
import app.katari.sync.ui.link.linkCodeEntryScreen
import app.katari.sync.ui.question.SyncQuestionsScreen
import app.katari.sync.ui.signin.SyncJoinOrigin
import app.katari.sync.ui.signin.SyncSignInScreen
import app.katari.sync.ui.status.rememberSyncHeadline
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.PreferenceScaffold
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.presentation.util.LocalBackPress
import mihon.app.ui.di.LocalAppUiGraph
import mihon.presentation.kit.dialog.ConfirmDialog
import mihon.presentation.kit.feedback.rememberScreenMessages
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The Sync page in Settings (D027, D028, D050), also opened from the Sync row in More and the Library toolbar. Before
 * setup it explains what syncs and offers the ways in; once linked it shows the status, the account's devices, the
 * options and leaving; after the device was removed or its account disabled, it says so and offers to sign in again.
 */
object SyncScreen : SearchableSettings, SyncJoinOrigin {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.sync_title

    /** What settings search finds here; the page itself shows more, which [Content] builds. */
    @Composable
    override fun getPreferences(): List<Preference> {
        val graph = LocalAppUiGraph.current
        return listOf(syncOptionsGroup(graph.syncPreferences, graph.networkConnectivity.knowsWifi))
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val backPress = LocalBackPress.current
        val model = metroViewModel<SyncViewModel>()
        val knowsWifi = LocalAppUiGraph.current.networkConnectivity.knowsWifi
        val status by model.status.collectAsState()
        val devices by model.devices.collectAsState()
        val canKeepSignIn by model.canKeepSignIn.collectAsState()
        val headline = rememberSyncHeadline()
        val messages = rememberScreenMessages()

        var dialog by rememberSaveable { mutableStateOf<SyncPageDialog?>(null) }
        val removeFailedMessage = stringResource(MR.strings.sync_remove_device_failed)
        val removeFailed by model.removeFailed.collectAsState()
        LaunchedEffect(removeFailed) { if (removeFailed > 0) messages.show(removeFailedMessage) }

        val actions = SyncPageActions(
            signIn = { server, account -> navigator.push(SyncSignInScreen(server, account)) },
            enterCode = { navigator.push(linkCodeEntryScreen()) },
            syncNow = model::syncNow,
            openChoices = { navigator.push(FirstSyncScreen) },
            openQuestions = { navigator.push(SyncQuestionsScreen) },
            linkDevice = { dialog = SyncPageDialog.LinkDevice },
            removeDevice = { dialog = SyncPageDialog.RemoveDevice(it.id.value, it.device.description.name) },
            disconnect = { dialog = SyncPageDialog.Disconnect },
            dismissEnded = model::dismissEnded,
        )

        PreferenceScaffold(
            titleRes = getTitleRes(),
            onBackPressed = backPress,
            itemsProvider = {
                when (val link = status?.link) {
                    null -> emptyList()
                    SyncStatus.Link.NotSetUp -> setUpPreferences(canKeepSignIn != false, actions)
                    is SyncStatus.Link.Ended -> endedPreferences(link, actions)
                    is SyncStatus.Link.Linked -> linkedPreferences(
                        link = link,
                        status = checkNotNull(status),
                        headline = headline,
                        devices = devices,
                        preferences = model.preferences,
                        knowsWifi = knowsWifi,
                        actions = actions,
                    )
                }
            },
        )

        when (val shown = dialog) {
            null -> Unit
            SyncPageDialog.LinkDevice -> LinkDeviceDialog(onDismissRequest = { dialog = null })
            is SyncPageDialog.RemoveDevice -> ConfirmDialog(
                title = stringResource(MR.strings.sync_remove_device_title, shown.name),
                text = stringResource(MR.strings.sync_remove_device_text, shown.name),
                confirmLabel = stringResource(MR.strings.action_remove),
                destructive = true,
                onConfirm = {
                    (devices as? SyncViewModel.DevicesState.Listed)?.devices
                        ?.firstOrNull { it.id.value == shown.id }
                        ?.let(model::removeDevice)
                    dialog = null
                },
                onDismissRequest = { dialog = null },
            )
            SyncPageDialog.Disconnect -> ConfirmDialog(
                title = stringResource(MR.strings.sync_disconnect_title),
                text = stringResource(
                    MR.strings.sync_disconnect_text,
                    (status?.link as? SyncStatus.Link.Linked)?.server?.host.orEmpty(),
                ),
                confirmLabel = stringResource(MR.strings.sync_action_disconnect),
                destructive = true,
                onConfirm = {
                    model.disconnect()
                    dialog = null
                },
                onDismissRequest = { dialog = null },
            )
        }
    }
}

/** What the page's rows do. */
internal class SyncPageActions(
    /** Opens sign-in, with the server address and account name to sign in to again, if any. */
    val signIn: (server: String?, account: String?) -> Unit,
    val enterCode: () -> Unit,
    val syncNow: () -> Unit,
    val openChoices: () -> Unit,
    val openQuestions: () -> Unit,
    val linkDevice: () -> Unit,
    val removeDevice: (app.katari.sync.client.account.AccountDevice) -> Unit,
    val disconnect: () -> Unit,
    val dismissEnded: () -> Unit,
)
