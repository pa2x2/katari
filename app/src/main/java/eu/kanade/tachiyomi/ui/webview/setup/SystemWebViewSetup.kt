package eu.kanade.tachiyomi.ui.webview.setup

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Android's web view is part of the system: there is nothing to set up, so it is always ready and nothing asks. */
@Inject
class SystemWebViewSetup : WebViewSetup {
    override val isManaged: Boolean = false
    override val state: StateFlow<WebViewSetupState> = MutableStateFlow(WebViewSetupState.Ready)
    override val wasNeeded: StateFlow<Boolean> = MutableStateFlow(false)
    override val isNoticed: StateFlow<Boolean> = MutableStateFlow(false)
    override val isRequested: StateFlow<Boolean> = MutableStateFlow(false)
    override val downloadBytes: Long = 0
    override val installedBytes: Long = 0

    override fun request() = Unit

    override fun dismissRequest() = Unit

    override fun dismissNotice() = Unit

    override fun start() = Unit

    override fun cancel() = Unit

    override suspend fun storedBytes(): Long = 0

    override suspend fun remove() = Unit
}
