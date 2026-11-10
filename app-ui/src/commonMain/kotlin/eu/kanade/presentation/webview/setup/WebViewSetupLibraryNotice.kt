package eu.kanade.presentation.webview.setup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Public
import app.katari.presentation.kit.surface.NoticeCard
import app.katari.presentation.kit.surface.NoticeTone
import app.katari.ui.di.LocalAppUiGraph
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The Library's word that a source needed a web view while none was set up, shown once a run: a source's request has
 * no screen to ask on, and fails without one. Set up opens the set-up dialog; Not now puts the notice away.
 */
@Composable
fun WebViewSetupLibraryNotice(modifier: Modifier = Modifier) {
    val setup = LocalAppUiGraph.current.webViewSetup
    val isNoticed by setup.isNoticed.collectAsStateWithLifecycle()
    if (!isNoticed) return
    NoticeCard(
        text = stringResource(MR.strings.webview_setup_notice),
        tone = NoticeTone.Info,
        modifier = modifier,
        icon = MaterialSymbols.Rounded.Public,
        actionLabel = stringResource(MR.strings.action_set_up),
        onAction = setup::request,
        dismissLabel = stringResource(MR.strings.action_not_now),
        onDismiss = setup::dismissNotice,
    )
}
