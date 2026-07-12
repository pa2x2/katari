package eu.kanade.tachiyomi.extension.api

import android.content.Context
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.extension.model.LoadResult
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import kotlinx.coroutines.flow.first
import mihon.core.common.GlobalCustomPreferences
import mihon.domain.extension.interactor.UpdateExtensionStores
import mihon.domain.extension.repository.ExtensionStoreRepository
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.util.lang.withIOContext
import uy.kohesive.injekt.injectLazy
import java.time.Instant
import kotlin.time.Duration.Companion.days

internal class ExtensionApi {

    private val repository: ExtensionStoreRepository by injectLazy()
    private val preferenceStore: PreferenceStore by injectLazy()
    private val updateExtensionStores: UpdateExtensionStores by injectLazy()
    private val extensionManager: ExtensionManager by injectLazy()
    private val customPreferences: GlobalCustomPreferences by injectLazy()

    private val lastExtCheck: Preference<Long> by lazy {
        preferenceStore.getLong(Preference.appStateKey("last_ext_check"), 0)
    }

    private data class UpdateCandidate(
        val installed: Extension.Installed,
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
    ): List<Extension.Installed>? {
        // Limit checks to once a day at most
        if (!fromAvailableExtensionList &&
            Instant.now().toEpochMilli() < lastExtCheck.get() + 1.days.inWholeMilliseconds
        ) {
            return null
        }

        updateExtensionStores()

        val extensions = if (fromAvailableExtensionList) {
            extensionManager.availableExtensionsFlow.value
        } else {
            findExtensions().also { lastExtCheck.set(Instant.now().toEpochMilli()) }
        }

        extensionManager.isInitialized.first { it }

        val extensionsByPkg = extensions.associateBy(Extension.Available::pkgName)

        val installedExtensions = ExtensionLoader.loadExtensions(context)
            .filterIsInstance<LoadResult.Success>()
            .map { it.extension }

        val updateCandidates = buildList {
            installedExtensions.forEach { installedExt ->
                val availableExt = extensionsByPkg[installedExt.pkgName] ?: return@forEach
                val hasUpdatedVer = availableExt.versionCode > installedExt.versionCode
                val hasUpdatedLib = ExtensionLoader.compareLibVersions(
                    availableExt.versionName,
                    installedExt.versionName,
                )?.let { it > 0 } == true
                val hasUpdate = hasUpdatedVer || hasUpdatedLib
                if (hasUpdate) {
                    add(UpdateCandidate(installedExt, availableExt))
                }
            }
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
    ): List<Extension.Installed> {
        val updatedExtensions = mutableListOf<Extension.Installed>()
        val leftoverExtensions = mutableListOf<Extension.Installed>()

        extensionManager.runAutoUpdateSession {
            updateCandidates.forEach { candidate ->
                val finalStep = runCatching {
                    extensionManager.installExtensionForAutoUpdate(candidate.available)
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
