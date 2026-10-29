package mihon.extension.smoke.run

import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.withTimeoutOrNull
import mihon.extension.smoke.results.InstallMeasurements
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/** How installing one listing ended, with what was measured on the way. */
internal sealed interface InstallAttempt {
    val measurements: InstallMeasurements

    data class Installed(val extension: Extension.Installed, override val measurements: InstallMeasurements) :
        InstallAttempt

    data class Failed(val error: InstallStep.Error, override val measurements: InstallMeasurements) : InstallAttempt

    data class TimedOut(val lastStep: InstallStep, override val measurements: InstallMeasurements) : InstallAttempt
}

/**
 * Installs a listing as the extension screens do, through the extension manager, and waits until the manager lists it
 * loaded or not. The install steps time the download, the conversion of an apk ([InstallStep.Converting] until
 * [InstallStep.Installed]) and the load after it.
 */
internal class ExtensionInstallation(private val manager: ExtensionManager, private val timeout: Duration) {

    suspend fun install(listing: Extension.Available): InstallAttempt {
        val steps = StepTimes()
        val outcome = withTimeoutOrNull(timeout) {
            manager.installExtension(listing)
                .takeWhile { step ->
                    steps.reached(step)
                    !step.isCompleted()
                }
                .collect {}
            (steps.last as? InstallStep.Error)?.let {
                return@withTimeoutOrNull InstallAttempt.Failed(it, steps.measured())
            }
            var installed = manager.getInstalledExtension(listing.pkgName)
            while (installed == null) {
                delay(POLL_INTERVAL)
                installed = manager.getInstalledExtension(listing.pkgName)
            }
            steps.loaded()
            InstallAttempt.Installed(installed, steps.measured())
        }
        if (outcome == null) manager.cancelInstallUpdateExtension(listing)
        return outcome ?: InstallAttempt.TimedOut(steps.last, steps.measured())
    }

    private class StepTimes {
        private val start = TimeSource.Monotonic.markNow()
        private var downloading: Duration? = null
        private var handedOver: Duration? = null
        private var converting: Duration? = null
        private var installed: Duration? = null
        private var loaded: Duration? = null

        var last: InstallStep = InstallStep.Pending
            private set

        fun reached(step: InstallStep) {
            last = step
            val now = start.elapsedNow()
            when (step) {
                InstallStep.Downloading -> downloading = downloading ?: now
                InstallStep.Installing -> handedOver = handedOver ?: now
                InstallStep.Converting -> {
                    // The step flow keeps only the latest step, so a short Installing may never be seen
                    handedOver = handedOver ?: now
                    converting = converting ?: now
                }
                InstallStep.Installed -> {
                    handedOver = handedOver ?: now
                    installed = now
                }
                else -> Unit
            }
        }

        fun loaded() {
            loaded = start.elapsedNow()
        }

        fun measured() = InstallMeasurements(
            downloadMs = millisBetween(downloading, handedOver),
            conversionMs = millisBetween(converting, installed),
            loadMs = millisBetween(installed, loaded),
        )

        private fun millisBetween(from: Duration?, to: Duration?): Long? {
            if (from == null || to == null) return null
            return (to - from).inWholeMilliseconds
        }
    }

    private companion object {
        val POLL_INTERVAL = 10.milliseconds
    }
}
