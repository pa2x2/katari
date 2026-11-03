package mihon.webview.jcef.page.challenge

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import mihon.core.common.web.ChallengePage
import mihon.core.common.web.ChallengePageEvents
import mihon.webview.jcef.cookie.ChromiumCookieMirror
import mihon.webview.jcef.runtime.JcefPresence
import mihon.webview.jcef.runtime.JcefRuntime
import okhttp3.HttpUrl
import java.util.concurrent.Executors

/**
 * Challenge pages on Chromium: each is a windowless browser of its own that nothing draws ([ChallengeBrowser]).
 * Chromium starts with the first page that needs it. A page sends the app's cookies, which the [cookies] mirror has
 * handed Chromium, and the cookies it sets are read back into the app's store before it reports a finished load.
 *
 * The pages run on a thread of their own, so a caller blocked waiting for one, on whichever other thread, never holds
 * it up. Pages open only where [runtime] has JCEF; this class names no JCEF type itself, as the app binds it on every
 * runtime.
 */
class ChromiumChallengePage(
    private val runtime: JcefRuntime,
    private val cookies: ChromiumCookieMirror,
) : ChallengePage {
    @Volatile
    private var pageThread: Thread? = null

    private val executor by lazy {
        Executors.newSingleThreadExecutor { task ->
            Thread(task, "Challenge pages").apply {
                isDaemon = true
                pageThread = this
            }
        }
    }
    private val scope by lazy { CoroutineScope(SupervisorJob() + executor.asCoroutineDispatcher()) }

    override val isOffered: Boolean
        get() = runtime.presence is JcefPresence.Present

    override fun open(
        url: HttpUrl,
        headers: Map<String, String>,
        userAgent: String,
        events: ChallengePageEvents,
    ): ChallengePage.Session? {
        if (Thread.currentThread() === pageThread) return null
        val browser = ChallengeBrowser(url, headers, userAgent, events, runtime, cookies, executor)
        val job = scope.launch { browser.run() }
        return ChallengePage.Session { job.cancel() }
    }
}
