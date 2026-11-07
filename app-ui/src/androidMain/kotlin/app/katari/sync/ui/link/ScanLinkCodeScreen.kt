package app.katari.sync.ui.link

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import app.katari.sync.client.account.LinkCodeLink
import app.katari.sync.ui.signin.SyncJoinViewModel
import app.katari.sync.ui.signin.joinProblemText
import app.katari.sync.ui.signin.leaveJoined
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.util.Screen
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Keyboard
import mihon.presentation.kit.content.ScanFrame
import mihon.presentation.kit.content.ScanFrameNote
import mihon.presentation.kit.feedback.rememberScreenMessages
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.list.RowGroupAction
import mihon.presentation.kit.list.RowGroupNote
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.Scaffold
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Scanning a linked device's code with the camera (D027). The camera asks for its permission first; a phone without
 * a camera, or a person who would rather not, types the code instead. A code read asks before joining.
 */
class ScanLinkCodeScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val model = metroViewModel<SyncJoinViewModel>()
        val join by model.join.collectAsState()
        val messages = rememberScreenMessages()
        val notALinkCode = stringResource(MR.strings.sync_not_a_link_code)

        var granted by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED,
            )
        }
        val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            granted = it
        }
        LaunchedEffect(Unit) { if (!granted) permission.launch(Manifest.permission.CAMERA) }
        var unavailable by rememberSaveable { mutableStateOf(false) }
        var scanned by rememberSaveable { mutableStateOf<String?>(null) }
        var lastRefused by remember { mutableStateOf<String?>(null) }
        val link = scanned?.let(LinkCodeLink::parse)

        LaunchedEffect(join) {
            (join as? SyncJoinViewModel.JoinState.Joined)?.let(navigator::leaveJoined)
        }

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.sync_scan_title),
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                )
            },
        ) { contentPadding ->
            RowList(contentPadding = contentPadding) {
                item {
                    val allowCamera = stringResource(MR.strings.sync_action_allow_camera)
                    ScanFrame(
                        note = when {
                            unavailable -> ScanFrameNote(stringResource(MR.strings.sync_camera_unavailable))
                            !granted -> ScanFrameNote(stringResource(MR.strings.sync_camera_needed), allowCamera) {
                                permission.launch(Manifest.permission.CAMERA)
                            }
                            else -> null
                        },
                    ) {
                        LinkCodeCamera(
                            scanning = link == null,
                            onText = { text ->
                                if (LinkCodeLink.parse(text) != null) {
                                    model.clearResult()
                                    scanned = text
                                } else if (text != lastRefused) {
                                    lastRefused = text
                                    messages.show(notALinkCode)
                                }
                            },
                            onUnavailable = { unavailable = true },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                item { RowGroupNote(stringResource(MR.strings.sync_scan_hint)) }
                item {
                    RowGroupAction(
                        label = stringResource(MR.strings.sync_action_enter_code_instead),
                        onClick = { navigator.push(EnterLinkCodeScreen()) },
                        icon = MaterialSymbols.Rounded.Keyboard,
                    )
                }
            }
        }

        if (link != null) {
            val failed = join as? SyncJoinViewModel.JoinState.Failed
            JoinConfirmDialog(
                link = link,
                joining = join == SyncJoinViewModel.JoinState.Joining,
                problem = failed?.let { joinProblemText(it.result, it.path) },
                onJoin = { model.redeemLinkCode(link.server.toString(), link.code) },
                onDismissRequest = { scanned = null },
            )
        }
    }
}
