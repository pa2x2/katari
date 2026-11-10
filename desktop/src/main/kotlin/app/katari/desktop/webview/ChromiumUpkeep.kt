package app.katari.desktop.webview

import app.katari.presentation.kit.feedback.AppMessages
import app.katari.webview.chromium.ChromiumInstallation
import app.katari.webview.chromium.ChromiumState
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tachiyomi.i18n.*
import tachiyomi.i18n.localized.stringResource
import tachiyomi.presentation.core.i18n.formatSize

/**
 * What the app does about Chromium as it starts: it removes the Chromium a build before this one left, which this
 * build can't run, and where that one was installed it downloads its own, since Chromium is what the user set the web
 * view up with. Nobody asked for that download just now, so on a metered network it fails instead of being made.
 */
@Inject
@SingleIn(AppScope::class)
class ChromiumUpkeep(private val chromium: ChromiumInstallation, private val messages: AppMessages) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        scope.launch {
            val replacesOlder = chromium.deleteOtherVersions()
            if (replacesOlder && chromium.state.value != ChromiumState.Installed) {
                val size = formatSize(chromium.release.archiveBytes, shorter = true)
                messages.show(stringResource(MR.strings.webview_setup_updating, size))
                chromium.install(allowMeteredNetwork = false)
            }
        }
    }
}
