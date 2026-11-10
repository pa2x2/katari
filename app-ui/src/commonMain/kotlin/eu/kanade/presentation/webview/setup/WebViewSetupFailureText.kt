package eu.kanade.presentation.webview.setup

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.ui.webview.setup.WebViewSetupFailure
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.shortFileSize
import tachiyomi.presentation.core.i18n.stringResource

/** Why the web view's set-up stopped, in a sentence. */
@Composable
fun webViewSetupFailureText(failure: WebViewSetupFailure): String = when (failure) {
    WebViewSetupFailure.MeteredNetwork -> stringResource(MR.strings.model_artifacts_failure_metered)
    is WebViewSetupFailure.InsufficientStorage -> stringResource(
        MR.strings.model_artifacts_failure_storage_space,
        shortFileSize(failure.requiredBytes),
        shortFileSize(failure.availableBytes),
    )
    WebViewSetupFailure.DamagedDownload -> stringResource(MR.strings.model_artifacts_failure_checksum)
    is WebViewSetupFailure.Network -> stringResource(
        MR.strings.model_artifacts_failure_network,
        failure.message ?: stringResource(MR.strings.model_artifacts_failure_unknown),
    )
    is WebViewSetupFailure.Storage -> stringResource(
        MR.strings.webview_setup_failure_storage,
        failure.message ?: stringResource(MR.strings.model_artifacts_failure_unknown),
    )
    WebViewSetupFailure.FolderRunsNoPrograms -> stringResource(MR.strings.webview_setup_failure_no_programs)
}
