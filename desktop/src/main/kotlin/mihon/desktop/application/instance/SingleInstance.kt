package mihon.desktop.application.instance

import kotlinx.coroutines.flow.Flow
import mihon.desktop.platform.DesktopOperatingSystem
import mihon.desktop.storage.DesktopDirectories

/**
 * Keeps the app to one running instance for each user, as Android keeps an app to one process: the preference files,
 * the database and the extension host each assume that this process is the only one writing them. A later launch hands
 * its request to the running instance and ends before it opens any of them.
 */
interface SingleInstance {
    /**
     * Makes this process the running instance, or hands this launch's request, opening [links], to the one that
     * already is.
     */
    fun claim(links: List<String>): InstanceClaim
}

sealed interface InstanceClaim {
    /** This process is the running instance until it ends. */
    interface Running : InstanceClaim {
        /** The request of each later launch, kept from the start of the run until it is collected, once. */
        val requests: Flow<InstanceRequest>

        /**
         * Stops answering later launches, which then wait for this process to end and become the running instance in
         * its place: a restart starts the next process before this one has ended.
         */
        fun stopAnswering()
    }

    /** Another instance runs and has this launch's request. */
    data object HandedOver : InstanceClaim

    /** Another instance holds the claim but took no request before the launch gave up waiting. */
    data object Unanswered : InstanceClaim
}

/** The [SingleInstance] of [operatingSystem], keeping its files in [directories]' runtime directory. */
fun systemSingleInstance(operatingSystem: DesktopOperatingSystem, directories: DesktopDirectories): SingleInstance {
    return when (operatingSystem) {
        DesktopOperatingSystem.LINUX -> LinuxSingleInstance(directories.runtime, System.getenv())
    }
}
