package eu.kanade.tachiyomi.extension.api

import android.content.Context
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.filterCompatibleWithApp
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.extension.store.findUpdate
import kotlinx.coroutines.flow.first
import mihon.core.common.GlobalCustomPreferences
import mihon.domain.extension.interactor.UpdateExtensionStores
import mihon.domain.extension.repository.ExtensionStoreRepository
import tachiyomi.core.common.util.lang.withIOContext
import uy.kohesive.injekt.injectLazy

internal class ExtensionApi {

    private val repository: ExtensionStoreRepository by injectLazy()
    private val updateExtensionStores: UpdateExtensionStores by injectLazy()
    private val extensionManager: ExtensionManager by injectLazy()
    private val customPreferences: GlobalCustomPreferences by injectLazy()

    private data class UpdateCandidate(
        val installed: Extension.Loaded,
        val available: Extension.Available,
    )

    suspend fun findExtensions(forceRefresh: Boolean = false): List<Extension.Available> {
        if (forceRefresh) {
            updateExtensionStores()
        }
        return withIOContext { repository.fetchExtensions() }
    }

    suspend fun checkForUpdates(
        context: Context,
        fromAvailableExtensionList: Boolean = false,
    ): List<Extension.Loaded>? {
        updateExtensionStores()

        val extensions = if (fromAvailableExtensionList) {
            extensionManager.availableExtensionListings
        } else {
            findExtensions().filterCompatibleWithApp()
        }

        // Only versions and signatures are read, so the extensions already loaded serve as they are
        val installedExtensions = extensionManager.getLoadedExtensions()

        val updateCandidates = installedExtensions.mapNotNull { installed ->
            installed.findUpdate(extensions)?.let { UpdateCandidate(installed, it) }
        }

        if (updateCandidates.isEmpty()) {
            extensionManager.setAvailableExtensions(extensions)
            return emptyList()
        }

        extensionManager.setAvailableExtensions(extensions)

        if (fromAvailableExtensionList || !customPreferences.extensionsAutoUpdates.get()) {
            val notifier = ExtensionUpdateNotifier(context)
            notifier.promptUpdates(updateCandidates.map { it.installed.name })
            return updateCandidates.map { it.installed }
        }

        return autoUpdateExtensions(context, updateCandidates)
    }

    private suspend fun autoUpdateExtensions(
        context: Context,
        updateCandidates: List<UpdateCandidate>,
    ): List<Extension.Loaded> {
        val updatedExtensions = mutableListOf<Extension.Loaded>()
        val leftoverExtensions = mutableListOf<Extension.Loaded>()

        extensionManager.runAutoUpdateSession {
            updateCandidates.forEach { candidate ->
                val finalStep = runCatching {
                    extensionManager.installExtensionForAutoUpdate(candidate.installed, candidate.available)
                        .first { it.isCompleted() }
                }.getOrElse { InstallStep.Error }

                when (finalStep) {
                    InstallStep.Installed -> updatedExtensions += candidate.installed
                    InstallStep.RequiresUserAction,
                    InstallStep.Idle,
                    InstallStep.Error,
                    -> leftoverExtensions += candidate.installed
                    else -> Unit
                }
            }
        }

        val notifier = ExtensionUpdateNotifier(context)
        notifier.autoUpdated(updatedExtensions.map { it.name })
        notifier.promptUpdates(leftoverExtensions.map { it.name })

        return updateCandidates.map { it.installed }
    }
}
