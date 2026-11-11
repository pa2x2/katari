package eu.kanade.presentation.webview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalUriHandler
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.automirroredrounded.ArrowBack
import app.katari.icons.materialsymbols.automirroredrounded.ArrowForward
import app.katari.icons.materialsymbols.rounded.Close
import app.katari.icons.materialsymbols.rounded.Cookie
import app.katari.icons.materialsymbols.rounded.OpenInBrowser
import app.katari.icons.materialsymbols.rounded.Refresh
import app.katari.icons.materialsymbols.rounded.Share
import app.katari.icons.materialsymbols.rounded.TabClose
import app.katari.platform.presentation.platformPresentation
import app.katari.presentation.kit.navigation.AppBar
import app.katari.presentation.kit.navigation.AppBarActions
import app.katari.presentation.kit.progress.LinearProgressIndicator
import app.katari.presentation.kit.shortcut.KeyboardShortcut
import app.katari.presentation.kit.shortcut.ScreenShortcuts
import app.katari.presentation.kit.shortcut.ShortcutGroup
import app.katari.presentation.kit.shortcut.ShortcutKeys
import app.katari.presentation.kit.surface.NoticeCard
import app.katari.presentation.kit.surface.NoticeTone
import app.katari.presentation.kit.theme.KatariTheme
import eu.kanade.tachiyomi.ui.webview.browser.BrowserLoading
import eu.kanade.tachiyomi.ui.webview.browser.BrowserPage
import tachiyomi.core.common.Constants
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The web view's bar: the shown [page]'s title and [url], its navigation and the page's menu, with a way to Cloudflare
 * help when [showCloudflareHelp] and the page's loading progress along the bottom. [onCloseTab] is set while a popup
 * is shown over the page that opened it; [onShare] where the platform can share links.
 */
@Composable
internal fun WebViewTopBar(
    page: BrowserPage,
    initialTitle: String?,
    url: String?,
    showCloudflareHelp: Boolean,
    onNavigateUp: () -> Unit,
    onCloseTab: (() -> Unit)?,
    onShare: (() -> Unit)?,
    onOpenInBrowser: () -> Unit,
    onClearCookies: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    Box {
        Column {
            AppBar(
                title = page.title ?: initialTitle,
                subtitle = url,
                navigateUp = onNavigateUp,
                navigationIcon = MaterialSymbols.Rounded.Close,
                // The page doesn't scroll the bar away, so it would stay large over the page for good.
                large = false,
                actions = {
                    AppBarActions(
                        listOfNotNull(
                            onCloseTab?.let {
                                AppBar.Action(
                                    title = stringResource(MR.strings.action_webview_close_tab),
                                    icon = MaterialSymbols.Rounded.TabClose,
                                    onClick = it,
                                )
                            },
                            AppBar.Action(
                                title = stringResource(MR.strings.action_webview_back),
                                icon = MaterialSymbols.AutoMirroredRounded.ArrowBack,
                                onClick = { if (page.canGoBack) page.goBack() },
                                enabled = page.canGoBack,
                            ),
                            AppBar.Action(
                                title = stringResource(MR.strings.action_webview_forward),
                                icon = MaterialSymbols.AutoMirroredRounded.ArrowForward,
                                onClick = { if (page.canGoForward) page.goForward() },
                                enabled = page.canGoForward,
                                shortcut = forwardShortcut(),
                            ),
                            AppBar.OverflowAction(
                                title = stringResource(MR.strings.action_webview_refresh),
                                onClick = page::reload,
                                icon = MaterialSymbols.Rounded.Refresh,
                                shortcut = reloadShortcut(),
                            ),
                            onShare?.let {
                                AppBar.OverflowAction(
                                    title = stringResource(MR.strings.action_share),
                                    onClick = it,
                                    icon = MaterialSymbols.Rounded.Share,
                                    group = 1,
                                )
                            },
                            AppBar.OverflowAction(
                                title = stringResource(MR.strings.action_open_in_browser),
                                onClick = onOpenInBrowser,
                                icon = MaterialSymbols.Rounded.OpenInBrowser,
                                group = 1,
                            ),
                            AppBar.OverflowAction(
                                title = stringResource(MR.strings.pref_clear_cookies),
                                onClick = onClearCookies,
                                icon = MaterialSymbols.Rounded.Cookie,
                                group = 2,
                            ),
                        ),
                    )
                },
            )

            if (showCloudflareHelp) {
                NoticeCard(
                    text = stringResource(platformPresentation.cloudflareHelpLabel),
                    tone = NoticeTone.Warning,
                    modifier = Modifier.padding(vertical = KatariTheme.spacing.s),
                    onClick = { uriHandler.openUri("${Constants.URL_HELP_TROUBLESHOOTING}#verify") },
                )
            }
        }
        when (val loading = page.loading) {
            BrowserLoading.Starting -> LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
            )
            is BrowserLoading.Loading -> {
                val progress = loading.progress
                if (progress == null) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                    )
                }
            }
            BrowserLoading.Finished -> Unit
        }
    }
}

// Back is the window's own, which goes through the page's history before it leaves the screen
@Composable
private fun forwardShortcut() = KeyboardShortcut(
    id = "webview.forward",
    label = stringResource(MR.strings.action_webview_forward),
    keys = listOf(ShortcutKeys.Chord(Key.DirectionRight, "→", alt = true)),
    group = ShortcutGroup.CurrentScreen,
)

// A browser's own reload keys beside the app's refresh key
@Composable
private fun reloadShortcut() = ScreenShortcuts.Refresh.let { refresh ->
    refresh.copy(
        label = stringResource(MR.strings.action_webview_refresh),
        keys = refresh.keys + ShortcutKeys.Chord(Key.R, "R", ctrl = true),
    )
}
