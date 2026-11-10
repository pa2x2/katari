package app.katari.webview.jcef.page.extension

import app.katari.core.common.web.ExtensionPage
import app.katari.core.common.web.ExtensionPageEvents
import app.katari.core.common.web.ExtensionPageSettings
import app.katari.core.common.web.ExtensionPages
import app.katari.webview.engine.cookie.ChromiumCookieMirror
import app.katari.webview.jcef.runtime.JcefPresence
import app.katari.webview.jcef.runtime.JcefRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

/**
 * Extensions' pages on Chromium, each a hidden browser of its own ([ExtensionBrowser]), which Chromium starts for when
 * the first of them needs it. The pages run on a thread of their own, which no extension code runs on: extensions hear
 * of their pages on threads the Android stand-ins choose. Pages open only where [runtime] has JCEF; this class names no
 * JCEF type itself, as the app binds it on every runtime.
 */
class ChromiumExtensionPages(
    private val runtime: JcefRuntime,
    private val cookies: ChromiumCookieMirror,
    private val userAgent: () -> String,
) : ExtensionPages {
    private val executor by lazy {
        Executors.newSingleThreadExecutor { task -> Thread(task, "Extension pages").apply { isDaemon = true } }
    }
    private val scope by lazy { CoroutineScope(SupervisorJob() + executor.asCoroutineDispatcher()) }

    override val isOffered: Boolean
        get() = runtime.presence is JcefPresence.Present

    override val defaultUserAgent: String
        get() = userAgent()

    override fun open(settings: ExtensionPageSettings, events: ExtensionPageEvents): ExtensionPage =
        ExtensionBrowser(settings, events, runtime, cookies, executor, scope)
}
