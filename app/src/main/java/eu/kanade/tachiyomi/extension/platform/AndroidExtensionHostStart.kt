package eu.kanade.tachiyomi.extension.platform

import dev.zacsweers.metro.Inject

/** Started from the outset: the app points Injekt at its graph as the app is created, before anything can build it. */
@Inject
class AndroidExtensionHostStart : ExtensionHostStart {
    override suspend fun awaitStarted() = Unit
}
