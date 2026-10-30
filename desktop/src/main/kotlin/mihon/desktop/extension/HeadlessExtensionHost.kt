package mihon.desktop.extension

import mihon.desktop.di.createDesktopGraph
import mihon.desktop.storage.DesktopDirectories

/**
 * The app's own graph over [directories], with the profiles loaded and the extension host started as the app starts
 * them, but no window, display or image loader: for developer tools that run extensions on folders of their own.
 */
suspend fun startHeadlessExtensionHost(directories: DesktopDirectories): ExtensionHostGraph {
    val graph = createDesktopGraph(directories)
    graph.profileManager.loadStartupSnapshot()
    graph.startExtensionHost()
    return graph
}
