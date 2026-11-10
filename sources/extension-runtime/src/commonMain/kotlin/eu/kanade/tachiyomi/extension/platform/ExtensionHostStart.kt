package eu.kanade.tachiyomi.extension.platform

import app.katari.core.common.platform.PlatformFacility

/**
 * When the platform has set up what extensions run on, the services they look up through Injekt among them. Whatever
 * injects the extension manager builds it, at any point of the app's start, so its first load waits for this instead
 * of relying on the order the app builds things in.
 */
@PlatformFacility
interface ExtensionHostStart {
    suspend fun awaitStarted()
}
