package mihon.core.common.web

/**
 * The app's [ChallengePage], one for the process, as Android's WebView is the device's: every
 * [eu.kanade.tachiyomi.network.interceptor.CloudflareInterceptor], the app's own and any an extension builds itself,
 * solves in it. Start-up installs it with the app's graph, before anything sends a request; it is asked for only once
 * a response is a challenge.
 */
object AppChallengePage {
    @Volatile
    private var installed: ChallengePage? = null

    fun install(page: ChallengePage) {
        synchronized(this) {
            check(installed == null) { "The app's challenge page is already installed" }
            installed = page
        }
    }

    val page: ChallengePage
        get() = checkNotNull(installed) { "A challenge came before start-up installed the app's challenge page" }
}
