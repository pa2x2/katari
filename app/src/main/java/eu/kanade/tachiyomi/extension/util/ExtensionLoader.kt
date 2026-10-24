package eu.kanade.tachiyomi.extension.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.content.pm.PackageInfoCompat
import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.kind.ExtensionKinds
import eu.kanade.tachiyomi.extension.kind.LoadPassSourceKeys
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.util.lang.Hash
import eu.kanade.tachiyomi.util.storage.copyAndSetReadOnlyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import logcat.LogPriority
import mihon.app.di.appGraph
import mihon.domain.extension.model.ContentWarning
import mihon.domain.extension.model.ExtensionStore
import tachiyomi.core.common.util.system.logcat
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
 */
internal object ExtensionLoader {

    @Suppress("DEPRECATION")
    private val PACKAGE_FLAGS = PackageManager.GET_CONFIGURATIONS or
        PackageManager.GET_META_DATA or
        PackageManager.GET_SIGNATURES or
        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else 0)

    private const val PRIVATE_EXTENSION_EXTENSION = "ext"

    private fun getPrivateExtensionDir(context: Context) = File(context.filesDir, "exts")

    /**
     * Throws with what stopped it when the extension can't be installed.
     */
    fun installPrivateExtensionFile(context: Context, file: File, extension: PackageInfo) {
        check(ExtensionKinds.isExtension(extension)) { "${extension.packageName} isn't an extension" }
        val currentExtension = getExtensionPackageInfoFromPkgName(context, extension.packageName)

        if (currentExtension != null) {
            check(
                PackageInfoCompat.getLongVersionCode(extension) >=
                    PackageInfoCompat.getLongVersionCode(currentExtension),
            ) { "The installed version is newer, and downgrading isn't allowed" }

            val extensionSignatures = getSignatures(extension)
            check(!extensionSignatures.isNullOrEmpty()) { "The extension isn't signed" }
            val currentSignatures =
                checkNotNull(getSignatures(currentExtension)) { "The installed extension isn't signed" }
            check(extensionSignatures.containsAll(currentSignatures)) {
                "It isn't signed with the same key as the installed extension"
            }
        }

        val target = File(getPrivateExtensionDir(context), "${extension.packageName}.$PRIVATE_EXTENSION_EXTENSION")
        try {
            target.delete()
            file.copyAndSetReadOnlyTo(target, overwrite = true)
        } catch (e: Exception) {
            target.delete()
            throw e
        }
        if (currentExtension != null) {
            ExtensionInstallReceiver.notifyReplaced(context, extension.packageName)
        } else {
            ExtensionInstallReceiver.notifyAdded(context, extension.packageName)
        }
    }

    fun uninstallPrivateExtension(context: Context, pkgName: String) {
        File(getPrivateExtensionDir(context), "$pkgName.$PRIVATE_EXTENSION_EXTENSION").delete()
    }

    fun getArchivePackageInfo(context: Context, file: File): PackageInfo? {
        return context.packageManager.getPackageArchiveInfo(file.absolutePath, PACKAGE_FLAGS)
    }

    /**
     * Return a list of all the available extensions initialized concurrently.
     *
     * @param context The application context.
     * @param alreadyLoaded Extensions loaded by an earlier call. Any of these whose apk is unchanged
     * and which still passes every check is returned as is, so its sources keep working and its
     * update status survives. Pass nothing to load every extension from scratch.
     */
    suspend fun loadExtensions(
        context: Context,
        alreadyLoaded: Map<String, Extension.Loaded> = emptyMap(),
    ): List<Extension.Installed> = withContext(Dispatchers.IO) {
        val graph = context.appGraph
        val policy = loadPolicy(graph.sourcePreferences, graph.extensionStoreRepository.getAllAsFlow().first())
        val trustExtension = graph.trustExtension
        val keys = LoadPassSourceKeys(graph.sourceIdentityRepository)
        val pkgManager = context.packageManager

        val installedPkgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pkgManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PACKAGE_FLAGS.toLong()))
        } else {
            pkgManager.getInstalledPackages(PACKAGE_FLAGS)
        }

        val sharedExtPkgs = installedPkgs
            .asSequence()
            .filter { ExtensionKinds.isExtension(it) }
            .map { ExtensionInfo(packageInfo = it, isShared = true) }

        // Read once: parsing an archive takes milliseconds, and it's looked up again for every package below
        val privateExtPkgs = getPrivateExtensionDir(context)
            .listFiles()
            .orEmpty()
            .filter { it.isFile && it.extension == PRIVATE_EXTENSION_EXTENSION }
            .mapNotNull {
                // Just in case, since Android 14+ requires them to be read-only
                if (it.canWrite()) {
                    it.setReadOnly()
                }

                val path = it.absolutePath
                pkgManager.getPackageArchiveInfo(path, PACKAGE_FLAGS)
                    ?.also { pkg -> pkg.applicationInfo?.fixBasePaths(path) }
            }
            .filter { ExtensionKinds.isExtension(it) }
            .map { ExtensionInfo(packageInfo = it, isShared = false) }
        val privateExtPkgsByName = privateExtPkgs.associateBy { it.packageInfo.packageName }

        val extPkgs = (sharedExtPkgs + privateExtPkgs)
            // Remove duplicates. Shared takes priority than private by default
            .distinctBy { it.packageInfo.packageName }
            // Compare version number
            .mapNotNull { sharedPkg ->
                val privatePkg = privateExtPkgsByName[sharedPkg.packageInfo.packageName]
                selectExtensionPackage(sharedPkg, privatePkg)
            }
            .toList()

        // Load each extension concurrently and wait for completion
        extPkgs
            .map {
                async {
                    loadExtensionCatching(
                        context,
                        it,
                        policy,
                        trustExtension,
                        keys,
                        alreadyLoaded[it.packageInfo.packageName],
                    )
                }
            }
            .awaitAll()
    }

    /**
     * Attempts to load an extension from the given package name. It checks if the extension
     * contains the required feature flag before trying to load it.
     */
    suspend fun loadExtensionFromPkgName(context: Context, pkgName: String): Extension.Installed? {
        val extensionPackage = getExtensionInfoFromPkgName(context, pkgName)
        if (extensionPackage == null) {
            logcat(LogPriority.ERROR) { "Extension package is not found ($pkgName)" }
            return null
        }
        val graph = context.appGraph
        val policy = loadPolicy(graph.sourcePreferences, graph.extensionStoreRepository.getAll())
        val keys = LoadPassSourceKeys(graph.sourceIdentityRepository)
        return loadExtensionCatching(context, extensionPackage, policy, graph.trustExtension, keys)
    }

    private fun loadPolicy(preferences: SourcePreferences, stores: List<ExtensionStore>) = LoadPolicy(
        stores = stores,
        enabledContentWarnings = preferences.enabledContentWarnings.get(),
        applyContentWarningsToInstalled = preferences.applyContentWarningsToInstalled.get(),
    )

    fun getExtensionPackageInfoFromPkgName(context: Context, pkgName: String): PackageInfo? {
        return getExtensionInfoFromPkgName(context, pkgName)?.packageInfo
    }

    private fun getExtensionInfoFromPkgName(context: Context, pkgName: String): ExtensionInfo? {
        val privateExtensionFile = File(getPrivateExtensionDir(context), "$pkgName.$PRIVATE_EXTENSION_EXTENSION")
        val privatePkg = if (privateExtensionFile.isFile) {
            context.packageManager.getPackageArchiveInfo(privateExtensionFile.absolutePath, PACKAGE_FLAGS)
                ?.takeIf { ExtensionKinds.isExtension(it) }
                ?.let {
                    it.applicationInfo?.fixBasePaths(privateExtensionFile.absolutePath)
                    ExtensionInfo(
                        packageInfo = it,
                        isShared = false,
                    )
                }
        } else {
            null
        }

        val sharedPkg = try {
            context.packageManager.getPackageInfo(pkgName, PACKAGE_FLAGS)
                .takeIf { ExtensionKinds.isExtension(it) }
                ?.let {
                    ExtensionInfo(
                        packageInfo = it,
                        isShared = true,
                    )
                }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }

        return selectExtensionPackage(sharedPkg, privatePkg)
    }

    /**
     * [loadExtension] reports the failures it knows how to name, but an apk can be malformed in
     * ways it doesn't check for. Keep anything unforeseen to the extension that caused it instead of
     * letting it take down the load of every other extension.
     */
    private suspend fun loadExtensionCatching(
        context: Context,
        extensionInfo: ExtensionInfo,
        policy: LoadPolicy,
        trustExtension: TrustExtension,
        keys: LoadPassSourceKeys,
        alreadyLoaded: Extension.Loaded? = null,
    ): Extension.Installed {
        return try {
            loadExtension(context, extensionInfo, policy, trustExtension, keys, alreadyLoaded)
        } catch (e: Throwable) {
            val pkgInfo = extensionInfo.packageInfo
            logcat(LogPriority.ERROR, e) { "Extension load error: ${pkgInfo.packageName}" }
            Extension.NotLoaded(
                name = pkgInfo.packageName,
                pkgName = pkgInfo.packageName,
                versionName = pkgInfo.versionName.orEmpty(),
                versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo),
                isShared = extensionInfo.isShared,
                contentWarning = ContentWarning.SAFE,
                signatures = getSignatures(pkgInfo).orEmpty(),
                reason = Extension.NotLoaded.Reason.Failed(e.rootMessage, e.stackTraceToString()),
            )
        }
    }

    /**
     * Loads an extension
     *
     * @param context The application context.
     * @param extensionInfo The extension to load.
     */
    private suspend fun loadExtension(
        context: Context,
        extensionInfo: ExtensionInfo,
        policy: LoadPolicy,
        trustExtension: TrustExtension,
        keys: LoadPassSourceKeys,
        alreadyLoaded: Extension.Loaded?,
    ): Extension.Installed {
        val pkgManager = context.packageManager
        val pkgInfo = extensionInfo.packageInfo
        val appInfo = pkgInfo.applicationInfo
        val metaData = appInfo?.metaData
        val pkgName = pkgInfo.packageName

        val versionName = pkgInfo.versionName
        val versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo)
        val signatures = getSignatures(pkgInfo).orEmpty()
        val (kind, description) = checkNotNull(
            ExtensionKinds.describe(pkgInfo, versionName.orEmpty(), metaData ?: Bundle.EMPTY) {
                appInfo?.let { pkgManager.getApplicationLabel(it).toString() } ?: pkgName
            },
        ) { "$pkgName isn't an extension" }
        val extName = description.name

        fun notLoaded(reason: Extension.NotLoaded.Reason) = Extension.NotLoaded(
            name = extName,
            pkgName = pkgName,
            versionName = versionName.orEmpty(),
            versionCode = versionCode,
            isShared = extensionInfo.isShared,
            contentWarning = description.contentWarning,
            signatures = signatures,
            libVersion = description.libVersion,
            reason = reason,
        )

        if (appInfo == null || metaData == null) {
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
        } else if (!trustExtension.isTrusted(pkgInfo, signatures, policy.stores)) {
            logcat(LogPriority.WARN) { "Extension $pkgName isn't trusted" }
            return notLoaded(Extension.NotLoaded.Reason.Untrusted(signatures.last()))
        }

        if (policy.applyContentWarningsToInstalled && description.contentWarning !in policy.enabledContentWarnings) {
            logcat(LogPriority.WARN) { "Extension $pkgName with ${description.contentWarning} not allowed" }
            return notLoaded(Extension.NotLoaded.Reason.Filtered)
        }

        // Everything above is cheap to check again, everything below isn't. Nothing about this apk
        // changed and it still passes, so keep the sources that are already registered for it.
        if (alreadyLoaded != null &&
            alreadyLoaded.versionCode == versionCode &&
            alreadyLoaded.isShared == extensionInfo.isShared
        ) {
            return alreadyLoaded
        }

        if (description.sourceClassNames.isEmpty()) {
            logcat(LogPriority.WARN) { "Missing source class for extension $extName" }
            return notLoaded(Extension.NotLoaded.Reason.Malformed)
        }

        val sources = try {
            kind.createSources(context, appInfo, pkgName, description, keys)
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            logcat(LogPriority.ERROR, e) { "Extension load error: $extName ($pkgName)" }
            return notLoaded(Extension.NotLoaded.Reason.Failed(e.rootMessage, e.stackTraceToString()))
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
            isShared = extensionInfo.isShared,
            signatures = signatures,
        )
    }

    /**
     * Choose which extension package to use based on version code
     *
     * @param shared extension installed to system
     * @param private extension installed to data directory
     */
    private fun selectExtensionPackage(shared: ExtensionInfo?, private: ExtensionInfo?): ExtensionInfo? {
        when {
            private == null && shared != null -> return shared
            shared == null && private != null -> return private
            shared == null && private == null -> return null
        }

        return if (PackageInfoCompat.getLongVersionCode(shared!!.packageInfo) >=
            PackageInfoCompat.getLongVersionCode(private!!.packageInfo)
        ) {
            shared
        } else {
            private
        }
    }

    /**
     * Returns the signatures of the package or null if it's not signed.
     *
     * @param pkgInfo The package info of the application.
     * @return List SHA256 digest of the signatures
     */
    fun getSignatures(pkgInfo: PackageInfo): List<String>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = pkgInfo.signingInfo
            when {
                signingInfo == null -> null
                signingInfo.hasMultipleSigners() -> signingInfo.apkContentsSigners
                else -> signingInfo.signingCertificateHistory
            }
        } else {
            @Suppress("DEPRECATION")
            pkgInfo.signatures
        }
            ?.map { Hash.sha256(it.toByteArray()) }
            ?.toList()
    }

    /**
     * On Android 13+ the ApplicationInfo generated by getPackageArchiveInfo doesn't
     * have sourceDir which breaks assets loading (used for getting icon here).
     */
    private fun ApplicationInfo.fixBasePaths(apkPath: String) {
        if (sourceDir == null) {
            sourceDir = apkPath
        }
        if (publicSourceDir == null) {
            publicSourceDir = apkPath
        }
    }

    private data class ExtensionInfo(
        val packageInfo: PackageInfo,
        val isShared: Boolean,
    )

    /**
     * What decides whether an extension may load, read once for every extension loaded together.
     */
    private data class LoadPolicy(
        val stores: List<ExtensionStore>,
        val enabledContentWarnings: Set<ContentWarning>,
        val applyContentWarningsToInstalled: Boolean,
    )
}

/**
 * The message of the deepest cause, which is the one that actually says what went wrong.
 */
private val Throwable.rootMessage: String
    get() {
        val root = generateSequence(this) { it.cause }.last()
        return listOfNotNull(root::class.simpleName, root.message).joinToString(": ")
    }
