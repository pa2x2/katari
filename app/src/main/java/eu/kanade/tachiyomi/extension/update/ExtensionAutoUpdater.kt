package eu.kanade.tachiyomi.extension.update

import eu.kanade.tachiyomi.extension.installer.ExtensionInstallOperation
import eu.kanade.tachiyomi.extension.installer.routing
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/**
 * Installs extension updates without asking, for users who turned that on. The updates are installed one at a
 * time in a session, and one the installer can't apply without the user, or that fails, is prompted instead.
 */
internal class ExtensionAutoUpdater(
    private val installer: ExtensionInstaller,
    private val notifier: ExtensionUpdateNotifier,
) {

    data class Candidate(
        val installed: Extension.Installed,
        val update: Extension.Available,
    )

    // The pending update count would otherwise count down through updates that are already being installed
    private val _isInProgress = MutableStateFlow(false)
    val isInProgress: StateFlow<Boolean> = _isInProgress.asStateFlow()

    /**
     * @param afterInstalls runs before the session ends, so it can catch the extensions up with what was installed
     * before the pending updates are counted again.
     */
    suspend fun install(candidates: List<Candidate>, afterInstalls: suspend () -> Unit) {
        _isInProgress.value = true
        try {
            val updated = mutableListOf<Extension.Installed>()
            val leftover = mutableListOf<Extension.Installed>()
            try {
                candidates.forEach { candidate ->
                    when (install(candidate)) {
                        InstallStep.Installed -> updated += candidate.installed
                        else -> leftover += candidate.installed
                    }
                }
            } finally {
                afterInstalls()
            }

            notifier.autoUpdated(updated.map { it.name })
            notifier.promptUpdates(leftover.map { it.name })
        } finally {
            _isInProgress.value = false
        }
    }

    private suspend fun install(candidate: Candidate): InstallStep {
        val routing = ExtensionInstallOperation.AutomaticUpdate(candidate.installed).routing()
        return try {
            installer.downloadAndInstall(candidate.update, routing.userActionBehavior, routing.installRoute)
                .first { it.isCompleted() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to update ${candidate.installed.pkgName}" }
            InstallStep.Error.from(e)
        }
    }
}
