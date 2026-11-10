package app.katari.desktop.webview.setup

import app.katari.core.metro.AppCoroutineScope
import app.katari.desktop.notification.DesktopNotification
import app.katari.desktop.notification.DesktopNotifications
import app.katari.desktop.notification.NotificationResponse
import app.katari.model.artifacts.api.state.ModelArtifactFailure
import app.katari.notification.AppNotificationKind
import app.katari.presentation.kit.feedback.AppMessages
import app.katari.webview.chromium.ChromiumFailure
import app.katari.webview.chromium.ChromiumInstallation
import app.katari.webview.chromium.ChromiumState
import app.katari.webview.jcef.runtime.JcefPresence
import app.katari.webview.jcef.runtime.JcefRuntime
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.ui.webview.setup.WebViewSetup
import eu.kanade.tachiyomi.ui.webview.setup.WebViewSetupFailure
import eu.kanade.tachiyomi.ui.webview.setup.WebViewSetupState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.i18n.*
import tachiyomi.i18n.localized.stringResource
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The web view's set-up on desktop: the download of the [chromium] it runs on. On a Java runtime that carries its own
 * Chromium, or has no JCEF to run one, there is nothing to set up and the web view counts as ready.
 *
 * A source's request that needs a web view has no screen to ask on: the first one each run that finds none set up
 * ([neededWithoutScreen]) puts a notice in the Library and a notification on the desktop, both leading to the set-up
 * dialog, and the request fails as it does without a web view.
 */
@Inject
@SingleIn(AppScope::class)
class DesktopWebViewSetup(
    private val chromium: ChromiumInstallation,
    private val runtime: JcefRuntime,
    private val messages: AppMessages,
    private val notifications: Lazy<DesktopNotifications>,
    @AppCoroutineScope scope: CoroutineScope,
) : WebViewSetup {
    // Whichever way it is answered holds for the run: installing and removing Chromium only move it between the two
    private val runsInstalledChromium by lazy {
        when (val presence = runtime.presence) {
            JcefPresence.NoChromium -> true
            is JcefPresence.Present -> presence.chromium != null
            JcefPresence.Absent -> false
        }
    }

    override val isManaged: Boolean
        get() = runsInstalledChromium

    override val state: StateFlow<WebViewSetupState> =
        chromium.state.map(::setupState).stateIn(scope, SharingStarted.Eagerly, setupState(chromium.state.value))

    private val noticed = MutableStateFlow(false)
    override val isNoticed: StateFlow<Boolean> = noticed.asStateFlow()

    private val requested = MutableStateFlow(false)
    override val isRequested: StateFlow<Boolean> = requested.asStateFlow()

    private val needed = MutableStateFlow(false)
    override val wasNeeded: StateFlow<Boolean> = needed.asStateFlow()

    private val hasNoticedThisRun = AtomicBoolean()

    override val downloadBytes: Long
        get() = chromium.release.archiveBytes

    override val installedBytes: Long
        get() = chromium.release.installedBytes

    init {
        scope.launch {
            var before = state.value
            state.collect { now ->
                if (now != WebViewSetupState.NotSetUp) takeNoticeAway()
                if (now == WebViewSetupState.Ready) needed.value = false
                if (now == WebViewSetupState.Ready && before != WebViewSetupState.Ready) {
                    messages.show(stringResource(MR.strings.webview_setup_ready))
                }
                before = now
            }
        }
    }

    /** Something with no screen of its own needed a web view just now. */
    fun neededWithoutScreen() {
        if (state.value == WebViewSetupState.Ready) return
        needed.value = true
        if (state.value != WebViewSetupState.NotSetUp || !hasNoticedThisRun.compareAndSet(false, true)) return
        noticed.value = true
        notifications.value.show(
            NOTIFICATION_KEY,
            DesktopNotification(
                kind = AppNotificationKind.COMMON,
                title = stringResource(MR.strings.webview_setup_title),
                body = stringResource(MR.strings.webview_setup_notice),
                onClick = NotificationResponse.Show(::request),
            ),
        )
    }

    override fun request() {
        takeNoticeAway()
        requested.value = true
    }

    override fun dismissRequest() {
        requested.value = false
    }

    override fun dismissNotice() = takeNoticeAway()

    override fun start() = chromium.install(allowMeteredNetwork = true)

    override fun cancel() = chromium.cancel()

    override suspend fun storedBytes(): Long = chromium.storedBytes()

    // Chromium's files can't go from under a Chromium that runs
    override suspend fun remove() {
        withContext(Dispatchers.IO) { runtime.stop() }
        chromium.delete()
    }

    private fun takeNoticeAway() {
        if (noticed.compareAndSet(expect = true, update = false)) notifications.value.withdraw(NOTIFICATION_KEY)
    }

    private fun setupState(state: ChromiumState): WebViewSetupState = when {
        !runsInstalledChromium -> WebViewSetupState.Ready
        else -> when (state) {
            ChromiumState.Installed -> WebViewSetupState.Ready
            ChromiumState.NotInstalled -> WebViewSetupState.NotSetUp
            is ChromiumState.Downloading -> WebViewSetupState.Downloading(state.downloadedBytes, state.totalBytes)
            ChromiumState.Unpacking -> WebViewSetupState.Unpacking
            is ChromiumState.Failed -> WebViewSetupState.Failed(state.failure.toSetupFailure())
        }
    }

    private fun ChromiumFailure.toSetupFailure(): WebViewSetupFailure = when (this) {
        is ChromiumFailure.Download -> when (val failure = failure) {
            ModelArtifactFailure.MeteredNetwork -> WebViewSetupFailure.MeteredNetwork
            is ModelArtifactFailure.InsufficientStorage ->
                WebViewSetupFailure.InsufficientStorage(failure.requiredBytes, failure.availableBytes)
            is ModelArtifactFailure.ChecksumMismatch -> WebViewSetupFailure.DamagedDownload
            is ModelArtifactFailure.Network -> WebViewSetupFailure.Network(failure.message)
            is ModelArtifactFailure.Storage -> WebViewSetupFailure.Storage(failure.message)
        }
        is ChromiumFailure.InsufficientStorage -> WebViewSetupFailure.InsufficientStorage(requiredBytes, availableBytes)
        ChromiumFailure.FolderRunsNoPrograms -> WebViewSetupFailure.FolderRunsNoPrograms
        is ChromiumFailure.Unpacking -> WebViewSetupFailure.Storage(message)
    }

    private companion object {
        const val NOTIFICATION_KEY = "web-view-setup"
    }
}
