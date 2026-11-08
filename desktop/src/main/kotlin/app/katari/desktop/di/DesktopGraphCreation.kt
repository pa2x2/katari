package app.katari.desktop.di

import app.katari.core.common.network.cookie.AppCookieStore
import app.katari.core.common.web.AppChallengePage
import app.katari.desktop.storage.DesktopAppPrivateDirectories
import app.katari.desktop.storage.DesktopDirectories
import dev.zacsweers.metro.createGraphFactory

/**
 * The app's graph over [directories], once a process. What the process keeps outside the graph, for code that reaches
 * it without one, is set up around it: first the app's cookie store, which every cookie jar uses, the app's own and
 * those extensions build, kept with the app's own files as Android's WebView keeps its cookies; then the graph's
 * challenge page, which every Cloudflare interceptor solves in.
 */
fun createDesktopGraph(directories: DesktopDirectories): DesktopGraph {
    AppCookieStore.open(DesktopAppPrivateDirectories(directories).files.toPath())
    return createGraphFactory<DesktopGraph.Factory>().create(directories).also { graph ->
        AppChallengePage.install(graph.challengePage)
    }
}
