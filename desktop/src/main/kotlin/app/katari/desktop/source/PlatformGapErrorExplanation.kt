package app.katari.desktop.source

import app.katari.core.platform.PlatformContext
import app.katari.extension.runtime.jvm.diagnostics.PlatformGaps
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.SourceErrorExplanation
import eu.kanade.presentation.util.isWebViewMember
import eu.kanade.presentation.util.platformGapSourceMessage
import eu.kanade.tachiyomi.ui.webview.setup.WebViewSetup
import eu.kanade.tachiyomi.ui.webview.setup.WebViewSetupState
import tachiyomi.i18n.*
import tachiyomi.i18n.localized.stringResource

/**
 * A source's call that failed because desktop doesn't provide yet what it needed of Android, named; or, where that is
 * a web view and the web view only waits to be set up, because of that.
 */
@Inject
class PlatformGapErrorExplanation(private val webViewSetup: WebViewSetup) : SourceErrorExplanation {
    override fun explain(context: PlatformContext, error: Throwable): String? {
        val missingMember = PlatformGaps.missingMember(error) ?: return null
        val awaitsSetup = isWebViewMember(missingMember) && webViewSetup.state.value != WebViewSetupState.Ready
        if (awaitsSetup) return stringResource(MR.strings.webview_setup_needed_by_source)
        return context.platformGapSourceMessage(missingMember)
    }
}
