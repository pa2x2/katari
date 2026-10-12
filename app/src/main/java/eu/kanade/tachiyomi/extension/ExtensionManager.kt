package eu.kanade.tachiyomi.extension

import android.content.Context
import android.graphics.drawable.Drawable
import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.domain.source.service.GlobalSourcePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.installer.ExtensionInstallOperation
import eu.kanade.tachiyomi.extension.installer.ExtensionInstallRouting
import eu.kanade.tachiyomi.extension.installer.routing
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.extension.store.findListing
import eu.kanade.tachiyomi.extension.store.findUpdate
import eu.kanade.tachiyomi.extension.store.pickStore
import eu.kanade.tachiyomi.extension.update.ExtensionAutoUpdater
import eu.kanade.tachiyomi.extension.update.ExtensionUpdateNotifier
import eu.kanade.tachiyomi.extension.util.ExtensionInstallReceiver
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import logcat.LogPriority
import mihon.core.common.GlobalCustomPreferences
import mihon.domain.extension.interactor.UpdateExtensionStores
import mihon.domain.extension.model.ExtensionStore
import mihon.domain.extension.repository.ExtensionStoreRepository
import mihon.feature.profiles.core.ProfileAwareStore
import mihon.feature.profiles.core.ProfileConstants
import mihon.feature.profiles.core.ProfileDatabase
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.source.model.StubSource
import tachiyomi.i18n.*
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
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
    private val extensionStoreRepository: ExtensionStoreRepository = Injekt.get(),
    private val updateExtensionStores: UpdateExtensionStores = Injekt.get(),
    private val customPreferences: GlobalCustomPreferences = Injekt.get(),
) {

    val scope = CoroutineScope(SupervisorJob())

    private val isInitialized = MutableStateFlow(false)

    // A full load replaces both maps, so every other change to them waits for it and applies on top instead of
    // being overwritten
    private val loadMutex = Mutex()

    // Trusting an extension is when its sources first show up, so they're hidden in the other profiles then
    private val trustedPkgNames = ConcurrentHashMap.newKeySet<String>()

    /**
     * The installer which installs, updates and uninstalls the extensions.
     */
    private val installer by lazy { ExtensionInstaller(context) }

    private val updateNotifier by lazy { ExtensionUpdateNotifier(context) }

    private val autoUpdater by lazy { ExtensionAutoUpdater(installer, updateNotifier) }

    private val iconMap = mutableMapOf<String, Drawable>()

    // Null until the stores first load
    @Volatile
    private var stores: List<ExtensionStore>? = null

    private val loadedExtensionMapFlow = MutableStateFlow(emptyMap<String, Extension.Loaded>())
    val loadedExtensionsFlow = loadedExtensionMapFlow.mapExtensionsOnceInitialized()

    // Every store's listing, since stores with different signing keys can list the same extension
    private val availableExtensionListFlow = MutableStateFlow(emptyList<Extension.Available>())
    val availableExtensionsFlow = availableExtensionListFlow
        .map { it.associateBy(Extension.Available::pkgName).values.toList() }
        .stateIn(scope, WhileSubscribed(5_000), emptyList())

    @Volatile
    private var hasFetchedListings = false

    private val notLoadedExtensionMapFlow = MutableStateFlow(emptyMap<String, Extension.NotLoaded>())
    val notLoadedExtensionsFlow = notLoadedExtensionMapFlow.mapExtensionsOnceInitialized()

    val isAutoUpdateInProgress: StateFlow<Boolean>
        get() = autoUpdater.isInProgress
    private val updateCheckInProgress = AtomicBoolean(false)
    private val _pendingUpdatesCount = MutableStateFlow(globalPreferences.extensionUpdatesCount.get())
    val pendingUpdatesCount: StateFlow<Int> = _pendingUpdatesCount.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) {
            loadExtensions()
            ExtensionInstallReceiver(InstallationListener()).register(context)

            // Everything the load decision rests on can change while running, so decide again. Content warnings
            // belong to the active profile, so switching to a profile that allows different ones reloads too.
            merge(
                trustExtension.changes(),
                combine(
                    preferences.enabledContentWarnings.changes(),
                    preferences.applyContentWarningsToInstalled.changes(),
                    ::Pair,
                )
                    .distinctUntilChanged()
                    .drop(1)
                    .map {},
            )
                .collectLatest { loadExtensions() }
        }

        // Store listings are only fetched now and then, so a store renamed or removed in between would stay on the
        // extensions that came from it. Assigned stores would be lost to the initial load if it finished later.
        scope.launch {
            isInitialized.first { it }
            extensionStoreRepository.getAllAsFlow().collect(::assignStores)
        }
    }

    private var subLanguagesEnabledOnFirstRun = preferences.enabledLanguages.isSet()

    suspend fun getLoadedExtensions(): List<Extension.Loaded> {
        isInitialized.first { it }
        return loadedExtensionMapFlow.value.values.toList()
    }

    suspend fun getNotLoadedExtensions(): List<Extension.NotLoaded> {
        isInitialized.first { it }
        return notLoadedExtensionMapFlow.value.values.toList()
    }

    fun getExtensionPackage(sourceId: Long): String? {
        return loadedExtensionMapFlow.value.values.find { extension ->
            extension.sources.any { it.id == sourceId }
        }
            ?.pkgName
    }

    fun getExtensionPackageAsFlow(sourceId: Long): Flow<String?> {
        return loadedExtensionsFlow.map { extensions ->
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
     * Loads and registers the installed extensions. Safe to call again: every extension is judged
     * again, so one can move between loaded and not loaded in either direction, while extensions
     * that still pass keep the instances they already had.
     */
    private suspend fun loadExtensions() = loadMutex.withLock {
        try {
            val extensions = ExtensionLoader.loadExtensions(context, loadedExtensionMapFlow.value)
            val loadedExtensions = extensions
                .filterIsInstance<Extension.Loaded>()
                .associateBy { it.pkgName }

            loadedExtensions.values
                .filter { it.pkgName in trustedPkgNames }
                .forEach { extension ->
                    trustedPkgNames -= extension.pkgName
                    initializeExtensionVisibility(
                        key = SourcePreferences.HIDDEN_SOURCES_KEY,
                        sourceIds = extension.sources.mapTo(HashSet()) { it.id.toString() },
                    )
                }

            // Newly loaded extensions have no status derived from the stores and their listings yet
            val availableExtensions = availableExtensionListFlow.value
            loadedExtensionMapFlow.value = loadedExtensions.mapValues { (_, extension) ->
                extension.withStatus(availableExtensions)
            }
            notLoadedExtensionMapFlow.value = extensions
                .filterIsInstance<Extension.NotLoaded>()
                .associate { it.pkgName to it.withStatus(availableExtensions) }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to load extensions" }
        } finally {
            // Anything waiting on the extensions would otherwise wait forever after a failed load
            isInitialized.value = true
        }
        updatePendingUpdatesCount()
    }

    /**
     * Finds the available extensions in the stores and updates [availableExtensionListFlow].
     *
     * @param forceRefresh whether to refresh the stores themselves first.
     */
    suspend fun findAvailableExtensions(forceRefresh: Boolean = false) {
        val extensions = try {
            if (forceRefresh) {
                updateExtensionStores()
            }
            fetchExtensions()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            withUIContext { context.toast(MR.strings.extension_api_error) }
            return
        }

        setAvailableExtensions(extensions)
    }

    /**
     * Refreshes the stores and their listings like [findAvailableExtensions], then installs or prompts the
     * updates of the installed extensions, loaded or not, as the user chose. Stays silent when the stores
     * can't be reached.
     */
    suspend fun checkForUpdates() {
        if (!updateCheckInProgress.compareAndSet(false, true)) return

        try {
            val extensions = try {
                updateExtensionStores()
                fetchExtensions()
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e)
                return
            }

            isInitialized.first { it }
            setAvailableExtensions(extensions)

            val availableExtensions = availableExtensionListFlow.value
            val candidates = (loadedExtensionMapFlow.value.values + notLoadedExtensionMapFlow.value.values)
                .filter { it.hasUpdate }
                .mapNotNull { installed ->
                    installed.findUpdate(availableExtensions)?.let { ExtensionAutoUpdater.Candidate(installed, it) }
                }
            if (candidates.isEmpty()) return

            if (customPreferences.extensionsAutoUpdates.get()) {
                // Install events may still be on their way when the installs finish
                autoUpdater.install(candidates, afterInstalls = ::loadExtensions)
            } else {
                updateNotifier.promptUpdates(candidates.map { it.installed.name })
            }
        } finally {
            updateCheckInProgress.set(false)
        }
    }

    private suspend fun fetchExtensions(): List<Extension.Available> {
        return withIOContext { extensionStoreRepository.fetchExtensions() }
    }

    private suspend fun setAvailableExtensions(extensions: List<Extension.Available>) {
        val compatibleExtensions = extensions.filterCompatibleWithApp()
        enableAdditionalSubLanguages(compatibleExtensions)
        availableExtensionListFlow.value = compatibleExtensions
        hasFetchedListings = true
        refreshStatuses()
        setupAvailableExtensionsSourcesDataMap(compatibleExtensions)
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
     * Returns a flow of the installation process for the given extension. It will complete
     * once the extension is installed or throws an error. The process will be canceled if
     * unsubscribed before its completion.
     *
     * @param extension The extension to be installed.
     */
    fun installExtension(extension: Extension.Available): Flow<InstallStep> {
        return installExtension(extension, ExtensionInstallOperation.NewInstall.routing())
    }

    /**
     * Returns a flow of the installation process for the given extension. It will complete
     * once the extension is updated or throws an error. The process will be canceled if
     * unsubscribed before its completion.
     *
     * @param extension The extension to be updated.
     */
    fun updateExtension(extension: Extension.Installed): Flow<InstallStep> {
        val update = extension.findUpdate(availableExtensionListFlow.value) ?: return emptyFlow()
        return installExtension(update, ExtensionInstallOperation.ManualUpdate(extension).routing())
    }

    private fun installExtension(
        extension: Extension.Available,
        routing: ExtensionInstallRouting,
    ): Flow<InstallStep> {
        return installer.downloadAndInstall(
            extension,
            routing.userActionBehavior,
            routing.installRoute,
        )
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
    fun uninstallExtension(extension: Extension.Installed) {
        installer.clearInstallStep(extension.pkgName)
        installer.uninstallApk(extension.pkgName)
    }

    /**
     * Adds the given extension to the list of trusted extensions. It also loads in background the
     * now trusted extensions.
     *
     * @param extension the extension to trust
     */
    fun trust(extension: Extension.NotLoaded) {
        val reason = extension.reason as? Extension.NotLoaded.Reason.Untrusted ?: return
        notLoadedExtensionMapFlow.value[extension.pkgName] ?: return

        // Loading it again is left to the reload triggered by the trust change
        trustedPkgNames += extension.pkgName
        trustExtension.trust(extension.pkgName, extension.versionCode, reason.signatureHash)
    }

    /**
     * Registers the given extension in this and the source managers.
     *
     * @param extension The extension to be registered.
     */
    private fun registerNewExtension(extension: Extension.Loaded) {
        val sourceIds = extension.sources.map { it.id.toString() }.toSet()
        initializeExtensionVisibility(
            key = SourcePreferences.HIDDEN_SOURCES_KEY,
            sourceIds = sourceIds,
        )
        loadedExtensionMapFlow.value += extension
        notLoadedExtensionMapFlow.value -= extension.pkgName
    }

    /**
     * Registers the given updated extension in this and the source managers previously removing
     * the outdated ones.
     *
     * @param extension The extension to be registered.
     */
    private fun registerUpdatedExtension(extension: Extension.Loaded) {
        val existingExtension = loadedExtensionMapFlow.value[extension.pkgName]
        val existingSourceIds = existingExtension?.sources
            ?.map { it.id.toString() }
            ?.toSet()
            .orEmpty()

        val sourceIds = extension.sources.map { it.id.toString() }.toSet()
        initializeExtensionVisibility(
            key = SourcePreferences.HIDDEN_SOURCES_KEY,
            sourceIds = sourceIds - existingSourceIds,
        )
        loadedExtensionMapFlow.value += extension
        notLoadedExtensionMapFlow.value -= extension.pkgName
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
        loadedExtensionMapFlow.value -= pkgName
        notLoadedExtensionMapFlow.value -= pkgName
    }

    /**
     * Listener which receives events of the extensions being installed, updated or removed.
     */
    private inner class InstallationListener : ExtensionInstallReceiver.Listener {

        override suspend fun onExtensionInstalled(extension: Extension.Loaded) = loadMutex.withLock {
            registerNewExtension(extension.withStatus(availableExtensionListFlow.value))
            updatePendingUpdatesCount()
        }

        override suspend fun onExtensionUpdated(extension: Extension.Loaded) = loadMutex.withLock {
            registerUpdatedExtension(extension.withStatus(availableExtensionListFlow.value))
            updatePendingUpdatesCount()
        }

        override suspend fun onExtensionNotLoaded(extension: Extension.NotLoaded) = loadMutex.withLock {
            loadedExtensionMapFlow.value -= extension.pkgName
            notLoadedExtensionMapFlow.value += extension.withStatus(availableExtensionListFlow.value)
            updatePendingUpdatesCount()
        }

        override suspend fun onPackageUninstalled(pkgName: String) = loadMutex.withLock {
            ExtensionLoader.uninstallPrivateExtension(context, pkgName)
            unregisterExtension(pkgName)
            updatePendingUpdatesCount()
        }
    }

    /**
     * Derives what the stores and their listings say about every installed extension, loaded or not.
     */
    private suspend fun refreshStatuses() = loadMutex.withLock {
        val availableExtensions = availableExtensionListFlow.value
        loadedExtensionMapFlow.value = loadedExtensionMapFlow.value.mapValues { (_, extension) ->
            extension.withStatus(availableExtensions)
        }
        notLoadedExtensionMapFlow.value = notLoadedExtensionMapFlow.value.mapValues { (_, extension) ->
            extension.withStatus(availableExtensions)
        }
        updatePendingUpdatesCount()
    }

    /**
     * Without any listing there's nothing to tell an obsolete extension by, so that's left as it was rather than
     * marking every extension obsolete.
     */
    private fun Extension.Loaded.withStatus(availableExtensions: List<Extension.Available>): Extension.Loaded {
        val listing = findListing(availableExtensions)
        return copy(
            hasUpdate = findUpdate(availableExtensions) != null,
            isObsolete = if (availableExtensions.isEmpty()) isObsolete else listing == null,
            store = currentStore(availableExtensions, fallback = listing?.store ?: store),
        )
    }

    /**
     * An extension that isn't loaded can still be updated, which is often what gets it loaded again.
     */
    private fun Extension.NotLoaded.withStatus(availableExtensions: List<Extension.Available>): Extension.NotLoaded {
        return copy(
            hasUpdate = findUpdate(availableExtensions) != null,
            store = currentStore(availableExtensions, fallback = findListing(availableExtensions)?.store ?: store),
        )
    }

    private suspend fun assignStores(stores: List<ExtensionStore>) {
        this.stores = stores
        refreshStatuses()
    }

    /**
     * Before the stores first load, [fallback] is the best guess.
     */
    private fun Extension.Installed.currentStore(
        availableExtensions: Collection<Extension.Available>,
        fallback: ExtensionStore?,
    ): ExtensionStore? {
        val stores = stores ?: return fallback
        return pickStore(stores, availableExtensions)
    }

    private fun updatePendingUpdatesCount() {
        // Until the extensions are loaded and the listings fetched nothing is known to have an update, so the count
        // kept from last time stays
        if (!isInitialized.value || !hasFetchedListings) return

        val pendingUpdateCount = (loadedExtensionMapFlow.value.values + notLoadedExtensionMapFlow.value.values)
            .count { it.hasUpdate }
        _pendingUpdatesCount.value = pendingUpdateCount
        globalPreferences.extensionUpdatesCount.set(pendingUpdateCount)
        if (pendingUpdateCount == 0) {
            updateNotifier.dismiss()
        }
    }

    private operator fun <T : Extension> Map<String, T>.plus(extension: T) = plus(extension.pkgName to extension)

    /**
     * Extensions are loaded in the background, and a state seeded at construction would hand out the empty
     * list from before that finished, so this only starts emitting once it did.
     */
    private fun <T : Extension> StateFlow<Map<String, T>>.mapExtensionsOnceInitialized(): Flow<List<T>> {
        return onStart { isInitialized.first { it } }.map { it.values.toList() }
    }
}

internal fun List<Extension.Available>.filterCompatibleWithApp(): List<Extension.Available> {
    return filter { ExtensionLoader.isRawLibVersionCompatible(it.libVersionName) }
}
