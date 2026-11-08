package eu.kanade.tachiyomi.extension.util

import app.katari.domain.extension.model.ContentWarning
import app.katari.domain.extension.repository.ExtensionStoreRepository
import dev.zacsweers.metro.Inject
import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.file.FileExtensionKind
import eu.kanade.tachiyomi.extension.file.FileExtensionLoader
import eu.kanade.tachiyomi.extension.file.FileExtensionStorage
import eu.kanade.tachiyomi.extension.file.StoredFileExtension
import eu.kanade.tachiyomi.extension.kind.ExtensionKinds
import eu.kanade.tachiyomi.extension.kind.LoadPassSourceKeys
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstalledForm
import eu.kanade.tachiyomi.extension.platform.ExtensionClassLoaders
import eu.kanade.tachiyomi.extension.platform.ExtensionFiles
import eu.kanade.tachiyomi.extension.platform.ExtensionMetaData
import eu.kanade.tachiyomi.extension.platform.ExtensionPackage
import eu.kanade.tachiyomi.extension.platform.ExtensionPackages
import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.source.identity.SourceIdentityRepository
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

/**
 * Class that handles the loading of the extensions. Supports two kinds of extensions:
 *
 * 1. Shared extension: This extension is installed to the system with package
 * installer, so other variants of Tachiyomi and its forks can also use this extension.
 *
 * 2. Private extension: This extension is put inside private data directory of the
 * running app, so this extension can only be used by the running app and not shared
 * with other apps.
 *
 * When both kinds of extensions are installed with a same package name, shared
 * extension will be used unless the version codes are different. In that case the
 * one with higher version code will be used.
 *
 * Extensions that are files rather than apks are always private, and [FileExtensionLoader] loads them.
 */
@Inject
class ExtensionLoader(
    private val packages: ExtensionPackages,
    private val classLoaders: ExtensionClassLoaders,
    private val files: ExtensionFiles,
    private val kinds: ExtensionKinds,
    private val sourcePreferences: SourcePreferences,
    private val extensionStoreRepository: ExtensionStoreRepository,
    private val trustExtension: TrustExtension,
    private val sourceIdentityRepository: SourceIdentityRepository,
) {

    internal fun getArchivePackage(file: File): ExtensionPackage? = packages.downloaded(file)

    /**
     * Return a list of all the available extensions, apks and files, initialized concurrently.
     *
     * @param alreadyLoaded Extensions loaded by an earlier call. Any of these whose apk is unchanged
     * and which still passes every check is returned as is, so its sources keep working and its
     * update status survives. Pass nothing to load every extension from scratch.
     */
    internal suspend fun loadExtensions(
        alreadyLoaded: Map<String, Extension.Loaded> = emptyMap(),
    ): List<Extension.Installed> = withContext(Dispatchers.IO) {
        val policy = ExtensionLoadPolicy.read(
            sourcePreferences,
            extensionStoreRepository.getAllAsFlow().first(),
        )
        val keys = LoadPassSourceKeys(sourceIdentityRepository)

        val sharedExtPkgs = packages.shared()
            .asSequence()
            .filter { kinds.isExtension(it) }

        // Read once: parsing an archive takes milliseconds, and it's looked up again for every package below
        val privateArchives = packages.private()
        val privateExtPkgs = privateArchives.values
            .filterNotNull()
            .filter { kinds.isExtension(it) }
        val privateExtPkgsByName = privateExtPkgs.associateBy { it.packageName }

        val extPkgs = (sharedExtPkgs + privateExtPkgs)
            // Remove duplicates. Shared takes priority than private by default
            .distinctBy { it.packageName }
            // Compare version number
            .mapNotNull { sharedPkg ->
                val privatePkg = privateExtPkgsByName[sharedPkg.packageName]
                selectExtensionPackage(sharedPkg, privatePkg)
            }
            .toList()

        // A shared package by the same name is loaded instead, and uninstalling that removes the file too
        val extPkgNames = extPkgs.mapTo(HashSet()) { it.packageName }
        val unreadablePrivateExts = privateArchives
            .filterValues { it == null }
            .keys
            .onEach { logcat(LogPriority.ERROR) { "Can't read the private extension file ${it.name}" } }
            .filter { it.nameWithoutExtension !in extPkgNames }
            .map { unreadablePrivateExtension(it) }

        val fileLoader = FileExtensionLoader(kinds, FileExtensionStorage(files), policy, trustExtension, keys)

        // Load each extension concurrently and wait for completion
        val loadedExts = extPkgs
            .map {
                async {
                    loadExtensionCatching(
                        it,
                        policy,
                        keys,
                        alreadyLoaded[it.packageName],
                    )
                }
            }
        val loadedFileExts = fileLoader.listInstalled()
            .map { stored ->
                async {
                    val pkgName = (stored as? StoredFileExtension.Readable)?.extension?.pkgName
                    fileLoader.load(stored, pkgName?.let(alreadyLoaded::get))
                }
            }
        unreadablePrivateExts + (loadedExts + loadedFileExts).awaitAll()
    }

    /**
     * Attempts to load an extension from the given package name. It checks if the extension
     * contains the required feature flag before trying to load it.
     */
    internal suspend fun loadExtensionFromPkgName(pkgName: String): Extension.Installed? {
        val file = kinds.ofFilePkgName(pkgName)
        if (file != null) return loadFileExtension(file.first, file.second)

        val extensionPackage = getExtensionPackageFromPkgName(pkgName)
        if (extensionPackage == null) {
            logcat(LogPriority.ERROR) { "Extension package is not found ($pkgName)" }
            return null
        }
        val policy = ExtensionLoadPolicy.read(sourcePreferences, extensionStoreRepository.getAll())
        val keys = LoadPassSourceKeys(sourceIdentityRepository)
        return loadExtensionCatching(extensionPackage, policy, keys)
    }

    private suspend fun loadFileExtension(kind: FileExtensionKind, id: String): Extension.Installed? {
        val storage = FileExtensionStorage(files)
        val stored = storage.readInstalled(kind, id)
        if (stored == null) {
            logcat(LogPriority.ERROR) { "File extension is not found (${kind.directoryName}:$id)" }
            return null
        }
        val policy = ExtensionLoadPolicy.read(sourcePreferences, extensionStoreRepository.getAll())
        val keys = LoadPassSourceKeys(sourceIdentityRepository)
        return FileExtensionLoader(kinds, storage, policy, trustExtension, keys).load(stored)
    }

    /** The apk extension installed as [pkgName], whichever of its shared package and private copy would load. */
    fun getExtensionPackageFromPkgName(pkgName: String): ExtensionPackage? {
        val privatePkg = packages.private(pkgName)
            ?.takeIf { kinds.isExtension(it) }

        val sharedPkg = packages.shared(pkgName)
            ?.takeIf { kinds.isExtension(it) }

        return selectExtensionPackage(sharedPkg, privatePkg)
    }

    /**
     * Without a package to read, the file's name is all there is to show, and it's the package name the file was
     * installed under, so uninstalling the row deletes it.
     */
    private fun unreadablePrivateExtension(file: File): Extension.NotLoaded {
        val pkgName = file.nameWithoutExtension
        return Extension.NotLoaded(
            name = pkgName,
            pkgName = pkgName,
            versionName = "",
            versionCode = 0,
            form = InstalledForm.Package(isShared = false, signatures = emptyList()),
            contentWarning = ContentWarning.SAFE,
            reason = Extension.NotLoaded.Reason.Unreadable,
        )
    }

    /**
     * [loadExtension] reports the failures it knows how to name, but an apk can be malformed in
     * ways it doesn't check for. Keep anything unforeseen to the extension that caused it instead of
     * letting it take down the load of every other extension.
     */
    private suspend fun loadExtensionCatching(
        extensionPackage: ExtensionPackage,
        policy: ExtensionLoadPolicy,
        keys: LoadPassSourceKeys,
        alreadyLoaded: Extension.Loaded? = null,
    ): Extension.Installed {
        return try {
            loadExtension(extensionPackage, policy, keys, alreadyLoaded)
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e) { "Extension load error: ${extensionPackage.packageName}" }
            Extension.NotLoaded(
                name = extensionPackage.packageName,
                pkgName = extensionPackage.packageName,
                versionName = extensionPackage.versionName.orEmpty(),
                versionCode = extensionPackage.versionCode,
                form = InstalledForm.Package(extensionPackage.isShared, extensionPackage.signatures.orEmpty()),
                contentWarning = ContentWarning.SAFE,
                reason = Extension.NotLoaded.Reason.Failed(e.rootMessage, e.stackTraceToString()),
            )
        }
    }

    /**
     * Loads an extension
     *
     * @param extensionPackage The extension to load.
     */
    private suspend fun loadExtension(
        extensionPackage: ExtensionPackage,
        policy: ExtensionLoadPolicy,
        keys: LoadPassSourceKeys,
        alreadyLoaded: Extension.Loaded?,
    ): Extension.Installed {
        val metaData = extensionPackage.metaData
        val pkgName = extensionPackage.packageName

        val versionName = extensionPackage.versionName
        val versionCode = extensionPackage.versionCode
        val signatures = extensionPackage.signatures.orEmpty()
        val form = InstalledForm.Package(extensionPackage.isShared, signatures)
        val (kind, description) = checkNotNull(
            kinds.describe(extensionPackage, versionName.orEmpty(), metaData ?: ExtensionMetaData.Empty) {
                extensionPackage.label()
            },
        ) { "$pkgName isn't an extension" }
        val extName = description.name

        fun notLoaded(reason: Extension.NotLoaded.Reason) = Extension.NotLoaded(
            name = extName,
            pkgName = pkgName,
            versionName = versionName.orEmpty(),
            versionCode = versionCode,
            form = form,
            contentWarning = description.contentWarning,
            libVersion = description.libVersion,
            reason = reason,
        )

        if (metaData == null) {
            logcat(LogPriority.WARN) { "Missing application info for extension $extName" }
            return notLoaded(Extension.NotLoaded.Reason.Malformed)
        }

        if (versionName.isNullOrEmpty()) {
            logcat(LogPriority.WARN) { "Missing versionName for extension $extName" }
            return notLoaded(Extension.NotLoaded.Reason.Malformed)
        }

        description.refusal?.let { refusal ->
            logcat(LogPriority.WARN) {
                "Extension $pkgName isn't loaded: $refusal (lib ${description.libVersionName})"
            }
            return notLoaded(refusal)
        }

        if (signatures.isEmpty()) {
            logcat(LogPriority.WARN) { "Package $pkgName isn't signed" }
            return notLoaded(Extension.NotLoaded.Reason.Unsigned)
        } else if (!trustExtension.isTrusted(pkgName, versionCode, signatures, policy.stores)) {
            logcat(LogPriority.WARN) { "Extension $pkgName isn't trusted" }
            return notLoaded(Extension.NotLoaded.Reason.Untrusted(signatures.last()))
        }

        if (policy.filters(description.contentWarning)) {
            logcat(LogPriority.WARN) { "Extension $pkgName with ${description.contentWarning} not allowed" }
            return notLoaded(Extension.NotLoaded.Reason.Filtered)
        }

        // Everything above is cheap to check again, everything below isn't. Nothing about this apk
        // changed and it still passes, so keep the sources that are already registered for it.
        if (alreadyLoaded != null &&
            alreadyLoaded.versionCode == versionCode &&
            (alreadyLoaded.form as? InstalledForm.Package)?.isShared == extensionPackage.isShared
        ) {
            return alreadyLoaded
        }

        if (description.sourceClassNames.isEmpty()) {
            logcat(LogPriority.WARN) { "Missing source class for extension $extName" }
            return notLoaded(Extension.NotLoaded.Reason.Malformed)
        }

        val sources = try {
            kind.createSources(extensionPackage, classLoaders, description, keys)
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            logcat(LogPriority.ERROR, e) { "Extension load error: $extName ($pkgName)" }
            return notLoaded(
                Extension.NotLoaded.Reason.Failed(
                    message = e.rootMessage,
                    stackTrace = e.stackTraceToString(),
                    missingPlatformMember = classLoaders.missingPlatformMember(e),
                ),
            )
        }

        val langs = sources.filterIsInstance<EntryCatalogueSource>()
            .map { it.lang }
            .toSet()
        val lang = when (langs.size) {
            0 -> ""
            1 -> langs.first()
            else -> "all"
        }

        return Extension.Loaded(
            name = extName,
            pkgName = pkgName,
            versionName = versionName,
            versionCode = versionCode,
            // A loaded extension's library was parsed by the kind that runs it
            libVersion = checkNotNull(description.libVersion),
            lang = lang,
            contentWarning = description.contentWarning,
            pkgFactory = description.factory,
            sources = sources,
            sourceIdScheme = kind.sourceIdScheme,
            providerKind = kind.providerKind,
            form = form,
        )
    }

    /**
     * Choose which extension package to use based on version code
     *
     * @param shared extension installed to system
     * @param private extension installed to data directory
     */
    private fun selectExtensionPackage(shared: ExtensionPackage?, private: ExtensionPackage?): ExtensionPackage? {
        when {
            private == null && shared != null -> return shared
            shared == null && private != null -> return private
            shared == null && private == null -> return null
        }

        return if (shared!!.versionCode >= private!!.versionCode) {
            shared
        } else {
            private
        }
    }
}
