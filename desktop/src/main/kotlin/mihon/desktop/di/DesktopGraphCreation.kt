package mihon.desktop.di

import dev.zacsweers.metro.createGraphFactory
import mihon.core.common.network.cookie.AppCookieStore
import mihon.desktop.storage.DesktopAppPrivateDirectories
import mihon.desktop.storage.DesktopDirectories

/**
 * The app's graph over [directories], once a process. What the process keeps outside the graph, for code that reaches
 * it without one, is set up first: the app's cookie store, which every cookie jar uses, the app's own and those
 * extensions build, kept with the app's own files as Android's WebView keeps its cookies.
 */
fun createDesktopGraph(directories: DesktopDirectories): DesktopGraph {
    AppCookieStore.open(DesktopAppPrivateDirectories(directories).files.toPath())
    return createGraphFactory<DesktopGraph.Factory>().create(directories)
}
