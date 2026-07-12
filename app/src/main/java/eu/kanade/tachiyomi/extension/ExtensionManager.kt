package eu.kanade.tachiyomi.extension

import android.content.Context
import android.graphics.drawable.Drawable
import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.domain.source.service.GlobalSourcePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.api.ExtensionApi
import eu.kanade.tachiyomi.extension.api.ExtensionUpdateNotifier
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.extension.model.LoadResult
import eu.kanade.tachiyomi.extension.util.ExtensionInstallReceiver
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller.UserActionBehavior
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.domain.extension.model.ExtensionStore
import mihon.feature.profiles.core.ProfileAwareStore
import mihon.feature.profiles.core.ProfileConstants
import mihon.feature.profiles.core.ProfileDatabase
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.source.model.StubSource
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The manager of extensions installed as another apk which extend the available sources. It handles
 * the retrieval of remotely available extensions as well as installing, updating and removing them.
 * To avoid malicious distribution, every extension must be signed and it will only be loaded if its
 * signature is trusted, otherwise the user will be prompted with a warning to trust it before being
 * loaded.
 */
class ExtensionManager(
    private val context: Context,
    private val preferences: SourcePreferences = Injekt.get(),
    private val globalPreferences: GlobalSourcePreferences = Injekt.get(),
    private val trustExtension: TrustExtension = Injekt.get(),
    private val profileDatabase: ProfileDatabase = Injekt.get(),
    private val profileStore: ProfileAwareStore = Injekt.get(),
) {

    val scope = CoroutineScope(SupervisorJob())

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    /**
     * API where all the available extensions can be found.
     */
    private val api = ExtensionApi()

    /**
     * The installer which installs, updates and uninstalls the extensions.
     */
    private val installer by lazy { ExtensionInstaller(context) }

    private val iconMap = mutableMapOf<String, Drawable>()

    private val installedExtensionMapFlow = MutableStateFlow(emptyMap<String, Extension.Installed>())
    val installedExtensionsFlow = installedExtensionMapFlow.mapExtensions(scope)

    private val availableExtensionMapFlow = MutableStateFlow(emptyMap<String, Extension.Available>())
    val availableExtensionsFlow = availableExtensionMapFlow.mapExtensions(scope)

    private val untrustedExtensionMapFlow = MutableStateFlow(emptyMap<String, Extension.Untrusted>())
    val untrustedExtensionsFlow = untrustedExtensionMapFlow.mapExtensions(scope)

    private val _isAutoUpdateInProgress = MutableStateFlow(false)
    val isAutoUpdateInProgress: StateFlow<Boolean> = _isAutoUpdateInProgress.asStateFlow()
    private val updateCheckInProgress = AtomicBoolean(false)
    private val _pendingUpdatesCount = MutableStateFlow(globalPreferences.extensionUpdatesCount.get())
    val pendingUpdatesCount: StateFlow<Int> = _pendingUpdatesCount.asStateFlow()

    init {
        initExtensions()
        ExtensionInstallReceiver(InstallationListener()).register(context)
    }

    private var subLanguagesEnabledOnFirstRun = preferences.enabledLanguages.isSet()

    fun getExtensionPackage(sourceId: Long): String? {
        return installedExtensionsFlow.value.find { extension ->
            extension.sources.any { it.id == sourceId }
        }
            ?.pkgName
    }

    fun getExtensionPackageAsFlow(sourceId: Long): Flow<String?> {
        return installedExtensionsFlow.map { extensions ->
            extensions.find { extension ->
                extension.sources.any { it.id == sourceId }
            }
                ?.pkgName
        }
    }

    fun getAppIconForSource(sourceId: Long): Drawable? {
        val pkgName = getExtensionPackage(sourceId) ?: return null

        return iconMap[pkgName] ?: iconMap.getOrPut(pkgName) {
            ExtensionLoader.getExtensionPackageInfoFromPkgName(context, pkgName)!!.applicationInfo!!
                .loadIcon(context.packageManager)
        }
    }

    private var availableExtensionsSourcesData: Map<Long, StubSource> = emptyMap()

    private fun setupAvailableExtensionsSourcesDataMap(extensions: List<Extension.Available>) {
        if (extensions.isEmpty()) return
        availableExtensionsSourcesData = extensions
            .flatMap { it.sources.map { source -> source.toStubSource() } }
            .associateBy { it.id }
    }

    fun getSourceData(id: Long) = availableExtensionsSourcesData[id]

    /**
     * Loads and registers the installed extensions.
     */
    private fun initExtensions() {
        val extensions = ExtensionLoader.loadExtensions(context)

        installedExtensionMapFlow.value = extensions
            .filterIsInstance<LoadResult.Success>()
            .associate { it.extension.pkgName to it.extension }

        untrustedExtensionMapFlow.value = extensions
            .filterIsInstance<LoadResult.Untrusted>()
            .associate { it.extension.pkgName to it.extension }

        _isInitialized.value = true
    }

    /**
     * Finds the available extensions in the [api] and updates [availableExtensionMapFlow].
     */
    suspend fun findAvailableExtensions(forceRefresh: Boolean = false) {
        val extensions: List<Extension.Available> = try {
            api.findExtensions(forceRefresh)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            withUIContext { context.toast(MR.strings.extension_api_error) }
            return
        }

        setAvailableExtensions(extensions)
    }

    fun setAvailableExtensions(extensions: List<Extension.Available>) {
        val compatibleExtensions = extensions.filterCompatibleWithApp()
        enableAdditionalSubLanguages(compatibleExtensions)
        availableExtensionMapFlow.value = compatibleExtensions.associateBy { it.pkgName }
        updatedInstalledExtensionsStatuses(compatibleExtensions)
        setupAvailableExtensionsSourcesDataMap(compatibleExtensions)
    }

    suspend fun checkForUpdates(
        context: Context,
        fromAvailableExtensionList: Boolean = false,
    ): List<Extension.Installed>? {
        if (!updateCheckInProgress.compareAndSet(false, true)) return null

        return try {
            api.checkForUpdates(context, fromAvailableExtensionList)
        } finally {
            updateCheckInProgress.set(false)
        }
    }

    suspend fun runAutoUpdateSession(block: suspend () -> Unit) {
        _isAutoUpdateInProgress.value = true
        try {
            block()
        } finally {
            try {
                refreshInstalledExtensionsUpdateStatus()
            } finally {
                _isAutoUpdateInProgress.value = false
            }
        }
    }

    /**
     * Enables the additional sub-languages in the app first run. This addresses
     * the issue where users still need to enable some specific languages even when
     * the device language is inside that major group. As an example, if a user
     * has a zh device language, the app will also enable zh-Hans and zh-Hant.
     *
     * If the user have already changed the enabledLanguages preference value once,
     * the new languages will not be added to respect the user enabled choices.
     */
    private fun enableAdditionalSubLanguages(extensions: List<Extension.Available>) {
        if (subLanguagesEnabledOnFirstRun || extensions.isEmpty()) {
            return
        }

        // Use the source lang as some aren't present on the extension level.
        val availableLanguages = extensions
            .flatMap(Extension.Available::sources)
            .distinctBy(Extension.Available.Source::lang)
            .map(Extension.Available.Source::lang)

        val deviceLanguage = Locale.getDefault().language
        val defaultLanguages = preferences.enabledLanguages.defaultValue()
        val languagesToEnable = availableLanguages.filter {
            it != deviceLanguage && it.startsWith(deviceLanguage)
        }

        preferences.enabledLanguages.set(defaultLanguages + languagesToEnable)
        subLanguagesEnabledOnFirstRun = true
    }

    /**
     * Sets the update field of the installed extensions with the given [availableExtensions].
     *
     * @param availableExtensions The list of extensions given by the [api].
     */
    private fun updatedInstalledExtensionsStatuses(availableExtensions: List<Extension.Available>) {
        if (availableExtensions.isEmpty()) {
            _pendingUpdatesCount.value = 0
            globalPreferences.extensionUpdatesCount.set(0)
            val notifier = ExtensionUpdateNotifier(context)
            notifier.dismiss()
            return
        }

        val installedExtensionsMap = reconcileInstalledExtensions(
            installedExtensionsMap = installedExtensionMapFlow.value,
            availableExtensions = availableExtensions,
        )
        if (installedExtensionsMap != installedExtensionMapFlow.value) {
            installedExtensionMapFlow.value = installedExtensionsMap
        }
        updatePendingUpdatesCount()
    }

    /**
     * Returns a flow of the installation process for the given extension. It will complete
     * once the extension is installed or throws an error. The process will be canceled if
     * unsubscribed before its completion.
     *
     * @param extension The extension to be installed.
     */
    fun installExtension(extension: Extension.Available): Flow<InstallStep> {
        return installer.downloadAndInstall(
            extension.apkUrl,
            extension,
            UserActionBehavior.LaunchPrompt,
        )
    }

    internal fun installExtensionForAutoUpdate(extension: Extension.Available): Flow<InstallStep> {
        return installer.downloadAndInstall(
            extension.apkUrl,
            extension,
            UserActionBehavior.MarkAsRequiresUserAction,
        )
    }

    /**
     * Returns a flow of the installation process for the given extension. It will complete
     * once the extension is updated or throws an error. The process will be canceled if
     * unsubscribed before its completion.
     *
     * @param extension The extension to be updated.
     */
    fun updateExtension(extension: Extension.Installed): Flow<InstallStep> {
        val availableExt = availableExtensionMapFlow.value[extension.pkgName] ?: return emptyFlow()
        return installExtension(availableExt)
    }

    fun cancelInstallUpdateExtension(extension: Extension) {
        installer.clearInstallStep(extension.pkgName)
        installer.cancelInstall(extension.pkgName)
    }

    /**
     * Sets to "installing" status of an extension installation.
     *
     * @param downloadId The id of the download.
     */
    fun setInstalling(downloadId: Long) {
        installer.updateInstallStep(downloadId, InstallStep.Installing)
    }

    fun updateInstallStep(downloadId: Long, step: InstallStep) {
        installer.updateInstallStep(downloadId, step)
    }

    fun installSteps() = installer.installSteps

    /**
     * Uninstalls the extension that matches the given package name.
     *
     * @param extension The extension to uninstall.
     */
    fun uninstallExtension(extension: Extension) {
        installer.clearInstallStep(extension.pkgName)
        installer.uninstallApk(extension.pkgName)
    }

    /**
     * Adds the given extension to the list of trusted extensions. It also loads in background the
     * now trusted extensions.
     *
     * @param extension the extension to trust
     */
    suspend fun trust(extension: Extension.Untrusted) {
        untrustedExtensionMapFlow.value[extension.pkgName] ?: return

        trustExtension.trust(extension.pkgName, extension.versionCode, extension.signatureHash)

        untrustedExtensionMapFlow.value -= extension.pkgName

        ExtensionLoader.loadExtensionFromPkgName(context, extension.pkgName)
            .let { it as? LoadResult.Success }
            ?.let { registerNewExtension(it.extension) }
    }

    /**
     * Registers the given extension in this and the source managers.
     *
     * @param extension The extension to be registered.
     */
    private fun registerNewExtension(extension: Extension.Installed) {
        val sourceIds = extension.sources.map { it.id.toString() }.toSet()
        initializeExtensionVisibility(
            key = SourcePreferences.HIDDEN_SOURCES_KEY,
            sourceIds = sourceIds,
        )
        installedExtensionMapFlow.value += extension.withObsolete(isObsolete = false)
    }

    /**
     * Registers the given updated extension in this and the source managers previously removing
     * the outdated ones.
     *
     * @param extension The extension to be registered.
     */
    private fun registerUpdatedExtension(extension: Extension.Installed) {
        val existingExtension = installedExtensionMapFlow.value[extension.pkgName]
        val existingSourceIds = existingExtension?.sources
            ?.map { it.id.toString() }
            ?.toSet()
            .orEmpty()

        val sourceIds = extension.sources.map { it.id.toString() }.toSet()
        initializeExtensionVisibility(
            key = SourcePreferences.HIDDEN_SOURCES_KEY,
            sourceIds = sourceIds - existingSourceIds,
        )
        installedExtensionMapFlow.value += extension.withObsolete(isObsolete = false)
    }

    private fun initializeExtensionVisibility(key: String, sourceIds: Set<String>) {
        if (sourceIds.isEmpty()) return

        scope.launch {
            val profiles = profileDatabase.getProfiles(includeArchived = true)
            val activeProfileId = profileStore.activeProfileId
            profiles.forEach { profile ->
                profileStore.profileStore(profile.id)
                    .getStringSet(key, emptySet())
                    .getAndSet { hiddenSources ->
                        when {
                            profile.id == activeProfileId -> hiddenSources - sourceIds
                            profile.id == ProfileConstants.DEFAULT_PROFILE_ID && profiles.size == 1 ->
                                hiddenSources -
                                    sourceIds
                            else -> hiddenSources + sourceIds
                        }
                    }
            }
        }
    }

    /**
     * Unregisters the extension in this and the source managers given its package name. Note this
     * method is called for every uninstalled application in the system.
     *
     * @param pkgName The package name of the uninstalled application.
     */
    private fun unregisterExtension(pkgName: String) {
        installer.clearInstallStep(pkgName)
        installedExtensionMapFlow.value -= pkgName
        untrustedExtensionMapFlow.value -= pkgName
    }

    /**
     * Listener which receives events of the extensions being installed, updated or removed.
     */
    private inner class InstallationListener : ExtensionInstallReceiver.Listener {

        override fun onExtensionInstalled(extension: Extension.Installed) {
            registerNewExtension(extension.withUpdateCheck())
            updatePendingUpdatesCount()
        }

        override fun onExtensionUpdated(extension: Extension.Installed) {
            registerUpdatedExtension(extension.withUpdateCheck())
            updatePendingUpdatesCount()
        }

        override fun onExtensionUntrusted(extension: Extension.Untrusted) {
            installedExtensionMapFlow.value -= extension.pkgName
            untrustedExtensionMapFlow.value += extension
            updatePendingUpdatesCount()
        }

        override fun onPackageUninstalled(pkgName: String) {
            ExtensionLoader.uninstallPrivateExtension(context, pkgName)
            unregisterExtension(pkgName)
            updatePendingUpdatesCount()
        }
    }

    /**
     * Extension method to set the update field of an installed extension.
     */
    private fun Extension.Installed.withUpdateCheck(): Extension.Installed {
        return if (updateExists()) {
            withUpdate(hasUpdate = true)
        } else {
            this
        }
    }

    private fun Extension.Installed.updateExists(availableExtension: Extension.Available? = null): Boolean {
        val availableExt = availableExtension
            ?: availableExtensionMapFlow.value[pkgName]
            ?: return false

        val hasUpdatedLib = ExtensionLoader.compareLibVersions(
            availableExt.versionName,
            versionName,
        )?.let { it > 0 } == true
        return availableExt.versionCode > versionCode || hasUpdatedLib
    }

    private fun updatePendingUpdatesCount() {
        val pendingUpdateCount = installedExtensionMapFlow.value.values.count { it.hasUpdate }
        _pendingUpdatesCount.value = pendingUpdateCount
        globalPreferences.extensionUpdatesCount.set(pendingUpdateCount)

        val notifier = ExtensionUpdateNotifier(context)
        if (pendingUpdateCount == 0) {
            notifier.dismiss()
        }
    }

    private suspend fun refreshInstalledExtensionsUpdateStatus() {
        val currentInstalledExtensions = installedExtensionMapFlow.value
        val reloadedInstalledExtensions = ExtensionLoader.loadExtensions(context)
            .filterIsInstance<LoadResult.Success>()
            .map { result ->
                val current = currentInstalledExtensions[result.extension.pkgName]
                result.extension
                    .withUpdate(hasUpdate = current?.hasUpdate ?: false)
                    .withObsolete(isObsolete = current?.isObsolete ?: false)
                    .withStore(store = current?.store)
            }
            .associateBy { it.pkgName }

        val availableExtensions = availableExtensionMapFlow.value.values.toList()
        installedExtensionMapFlow.value = if (availableExtensions.isEmpty()) {
            reloadedInstalledExtensions
        } else {
            reconcileInstalledExtensions(
                installedExtensionsMap = reloadedInstalledExtensions,
                availableExtensions = availableExtensions,
            )
        }
        updatePendingUpdatesCount()
    }

    private fun reconcileInstalledExtensions(
        installedExtensionsMap: Map<String, Extension.Installed>,
        availableExtensions: List<Extension.Available>,
    ): Map<String, Extension.Installed> {
        val availableExtensionsMap = availableExtensions.associateBy(Extension.Available::pkgName)

        return installedExtensionsMap.mapValues { (pkgName, extension) ->
            val availableExtension = availableExtensionsMap[pkgName]

            if (availableExtension == null) {
                extension
                    .withUpdate(hasUpdate = false)
                    .withObsolete(isObsolete = true)
            } else {
                extension
                    .withUpdate(hasUpdate = extension.updateExists(availableExtension))
                    .withObsolete(isObsolete = false)
                    .withStore(store = availableExtension.store)
            }
        }
    }

    private fun Extension.Installed.withUpdate(hasUpdate: Boolean): Extension.Installed {
        return copy(hasUpdate = hasUpdate)
    }

    private fun Extension.Installed.withObsolete(isObsolete: Boolean): Extension.Installed {
        return copy(isObsolete = isObsolete)
    }

    private fun Extension.Installed.withStore(store: ExtensionStore?): Extension.Installed {
        return copy(store = store)
    }

    private operator fun <T : Extension> Map<String, T>.plus(extension: T) = plus(extension.pkgName to extension)

    private fun <T : Extension> StateFlow<Map<String, T>>.mapExtensions(
        scope: CoroutineScope,
        transform: (Collection<T>) -> List<T> = { it.toList() },
    ): StateFlow<List<T>> {
        return map { transform(it.values) }.stateIn(scope, WhileSubscribed(5_000), transform(value.values))
    }
}

internal fun pendingExtensionUpdateCount(extensions: Collection<Extension.Installed>): Int {
    return extensions.count { it.hasUpdate }
}

internal fun List<Extension.Available>.filterCompatibleWithApp(): List<Extension.Available> {
    return filter { ExtensionLoader.isRawLibVersionCompatible(it.libVersionName) }
}
