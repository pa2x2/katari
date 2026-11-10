package eu.kanade.presentation.more.settings.screen.advanced

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.katari.presentation.kit.dialog.ConfirmDialog
import app.katari.ui.di.LocalAppUiGraph
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.PreferenceScaffold
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.webview.setup.webViewSetupFailureText
import eu.kanade.tachiyomi.ui.webview.setup.WebViewSetup
import eu.kanade.tachiyomi.ui.webview.setup.WebViewSetupState
import kotlinx.coroutines.launch
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.shortFileSize
import tachiyomi.presentation.core.i18n.stringResource

/**
 * What the web view runs on, where that is the app's to manage: whether Chromium is there, its download, the disk
 * space it takes and its removal, and the data the web view keeps.
 */
class WebViewEngineScreen : Screen() {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val graph = LocalAppUiGraph.current
        val setup = graph.webViewSetup
        val scope = rememberCoroutineScope()
        val state by setup.state.collectAsStateWithLifecycle()
        val storedBytes by produceState(0L, state) { value = setup.storedBytes() }
        val isBusy = state is WebViewSetupState.Downloading || state == WebViewSetupState.Unpacking
        var confirmsRemoval by rememberSaveable { mutableStateOf(false) }
        val clearWebViewData = rememberClearWebViewData()

        PreferenceScaffold(
            titleRes = MR.strings.webview_engine_title,
            onBackPressed = navigator::pop,
            itemsProvider = {
                listOf(
                    Preference.PreferenceItem.TextPreference(
                        title = CHROMIUM,
                        subtitle = webViewEngineStatus(setup),
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.action_download),
                        visible = state == WebViewSetupState.NotSetUp,
                        onClick = setup::request,
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.action_retry),
                        visible = state is WebViewSetupState.Failed,
                        onClick = setup::start,
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.webview_engine_cancel_download),
                        visible = state is WebViewSetupState.Downloading,
                        onClick = setup::cancel,
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.webview_engine_remove),
                        subtitle = stringResource(MR.strings.webview_engine_remove_summary, shortFileSize(storedBytes)),
                        visible = storedBytes > 0 && !isBusy,
                        onClick = { confirmsRemoval = true },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_clear_webview_data),
                        visible = graph.webViewData.isOffered,
                        onClick = clearWebViewData,
                    ),
                    Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.webview_engine_info)),
                )
            },
        )

        if (confirmsRemoval) {
            ConfirmDialog(
                title = stringResource(MR.strings.webview_engine_remove),
                text = stringResource(MR.strings.webview_engine_remove_confirm),
                confirmLabel = stringResource(MR.strings.action_remove),
                onConfirm = {
                    confirmsRemoval = false
                    scope.launch { setup.remove() }
                },
                onDismissRequest = { confirmsRemoval = false },
                destructive = true,
            )
        }
    }

    private companion object {
        // A name, the same in every language
        const val CHROMIUM = "Chromium"
    }
}

/** Where the web view's engine stands, in a line: installed with its size, not set up, how far a download is, why it failed. */
@Composable
fun webViewEngineStatus(setup: WebViewSetup): String {
    val state by setup.state.collectAsStateWithLifecycle()
    return when (val current = state) {
        WebViewSetupState.Ready -> stringResource(
            MR.strings.webview_engine_installed,
            shortFileSize(setup.installedBytes),
        )
        WebViewSetupState.NotSetUp -> stringResource(MR.strings.webview_engine_not_set_up)
        is WebViewSetupState.Downloading -> stringResource(
            MR.strings.model_artifacts_state_downloading,
            shortFileSize(current.downloadedBytes),
            shortFileSize(current.totalBytes),
        )
        WebViewSetupState.Unpacking -> stringResource(MR.strings.webview_setup_unpacking)
        is WebViewSetupState.Failed -> webViewSetupFailureText(current.failure)
    }
}
