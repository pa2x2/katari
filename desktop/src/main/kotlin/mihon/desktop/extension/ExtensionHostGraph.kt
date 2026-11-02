package mihon.desktop.extension

import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.kind.ExtensionCompatibility
import eu.kanade.tachiyomi.extension.kind.ExtensionKinds
import eu.kanade.tachiyomi.extension.platform.HostedExtensionFamilies
import mihon.app.di.injekt.ExtensionInjektGraph
import mihon.app.di.injekt.MetroInjektRegistrar
import mihon.domain.extension.repository.ExtensionStoreRepository
import mihon.extension.compat.AndroidCompatEnvironment
import mihon.extension.runtime.jvm.application.ExtensionApplication
import mihon.extension.runtime.jvm.application.MainLooperThread
import mihon.extension.runtime.jvm.loading.ExtensionUncaughtErrors
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope

/**
 * The part of the app's graph that runs extensions, which the app starts before its window and which developer tools
 * such as `:tools:extension-compatibility-harness` start without one ([startHeadlessExtensionHost]). The stores, kinds
 * and families are what those tools read to install from the stores the way the extension screens do.
 */
interface ExtensionHostGraph : ExtensionInjektGraph {
    val androidCompatEnvironment: AndroidCompatEnvironment
    val extensionApplication: ExtensionApplication
    val extensionManager: ExtensionManager
    val extensionStoreRepository: ExtensionStoreRepository
    val extensionKinds: ExtensionKinds
    val extensionCompatibility: ExtensionCompatibility
    val hostedExtensionFamilies: HostedExtensionFamilies
}

/**
 * Extensions run on the Android stand-ins, whose environment and main thread come first. Injekt then hands them the
 * graph's services, and building the extension manager loads the installed ones. The active profile must be loaded
 * first, since extensions read its source settings. Errors of extensions' own background work stop at
 * [ExtensionUncaughtErrors], in front of the crash handling installed before.
 */
fun ExtensionHostGraph.startExtensionHost() {
    ExtensionUncaughtErrors.install()
    prepareExtensionHost()
    extensionManager
}

/**
 * Sets up what extensions run on but loads none, so the extension manager loads them once something first asks for it,
 * as the crash screen's debug info does.
 */
fun ExtensionHostGraph.prepareExtensionHost() {
    AndroidCompatEnvironment.install(androidCompatEnvironment)
    MainLooperThread.start()
    Injekt = InjektScope(MetroInjektRegistrar(application = extensionApplication, graph = { this }))
}
