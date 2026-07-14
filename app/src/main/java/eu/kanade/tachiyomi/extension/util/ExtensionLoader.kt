package eu.kanade.tachiyomi.extension.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import dalvik.system.DelegateLastClassLoader
import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.LoadResult
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory
import eu.kanade.tachiyomi.source.adapter.asUnifiedSource
import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.EntrySourceFactory
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.util.lang.Hash
import eu.kanade.tachiyomi.util.storage.copyAndSetReadOnlyTo
import eu.kanade.tachiyomi.util.system.ChildFirstPathClassLoader
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.injectLazy
import java.io.File

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

    private val preferences: SourcePreferences by injectLazy()
    private val trustExtension: TrustExtension by injectLazy()
    private val loadNsfwSource by lazy {
        preferences.showNsfwSource.get()
    }

    private const val EXTENSION_FEATURE = "tachiyomi.extension"
    private const val METADATA_SOURCE_CLASS = "tachiyomi.extension.class"
    private const val METADATA_SOURCE_FACTORY = "tachiyomi.extension.factory"
    private const val METADATA_NSFW = "tachiyomi.extension.nsfw"

    private const val METADATA_NAME = "tachiyomix.name"
    private const val METADATA_EXTENSION_LIB = "tachiyomix.extensionLib"
    private const val METADATA_CONTENT_WARNING = "tachiyomix.contentWarning"

    private val UPSTREAM_SOURCE_API_VERSION_RANGES = listOf(
        libVersionRange("1.4", "1.4"),
        libVersionRange("1.6", "1.6"),
    )
    private val ENTRY_SOURCE_API_VERSION_FAMILIES = listOf(
        LibVersion.parse("2.0")!!,
        LibVersion.parse("2.1")!!,
    )

    @Suppress("DEPRECATION")
    private val PACKAGE_FLAGS = PackageManager.GET_CONFIGURATIONS or
        PackageManager.GET_META_DATA or
        PackageManager.GET_SIGNATURES or
        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else 0)

    private const val PRIVATE_EXTENSION_EXTENSION = "ext"

    private fun getPrivateExtensionDir(context: Context) = File(context.filesDir, "exts")

    fun installPrivateExtensionFile(context: Context, file: File): Boolean {
        val extension = context.packageManager.getPackageArchiveInfo(file.absolutePath, PACKAGE_FLAGS)
            ?.takeIf { isPackageAnExtension(it) } ?: return false
        val currentExtension = getExtensionPackageInfoFromPkgName(context, extension.packageName)

        if (currentExtension != null) {
            if (PackageInfoCompat.getLongVersionCode(extension) <
                PackageInfoCompat.getLongVersionCode(currentExtension)
            ) {
                logcat(LogPriority.ERROR) { "Installed extension version is higher. Downgrading is not allowed." }
                return false
            }

            val extensionSignatures = getSignatures(extension)
            if (extensionSignatures.isNullOrEmpty()) {
                logcat(LogPriority.ERROR) { "Extension to be installed is not signed." }
                return false
            }

            if (!extensionSignatures.containsAll(getSignatures(currentExtension)!!)) {
                logcat(LogPriority.ERROR) { "Installed extension signature is not matched." }
                return false
            }
        }

        val target = File(getPrivateExtensionDir(context), "${extension.packageName}.$PRIVATE_EXTENSION_EXTENSION")
        return try {
            target.delete()
            file.copyAndSetReadOnlyTo(target, overwrite = true)
            if (currentExtension != null) {
                ExtensionInstallReceiver.notifyReplaced(context, extension.packageName)
            } else {
                ExtensionInstallReceiver.notifyAdded(context, extension.packageName)
            }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to copy extension file." }
            target.delete()
            false
        }
    }

    fun uninstallPrivateExtension(context: Context, pkgName: String) {
        File(getPrivateExtensionDir(context), "$pkgName.$PRIVATE_EXTENSION_EXTENSION").delete()
    }

    /**
     * Return a list of all the available extensions initialized concurrently.
     *
     * @param context The application context.
     */
    fun loadExtensions(context: Context): List<LoadResult> {
        val pkgManager = context.packageManager

        val installedPkgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pkgManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PACKAGE_FLAGS.toLong()))
        } else {
            pkgManager.getInstalledPackages(PACKAGE_FLAGS)
        }

        val sharedExtPkgs = installedPkgs
            .asSequence()
            .filter { isPackageAnExtension(it) }
            .map { ExtensionInfo(packageInfo = it, isShared = true) }

        val privateExtPkgs = getPrivateExtensionDir(context)
            .listFiles()
            ?.asSequence()
            ?.filter { it.isFile && it.extension == PRIVATE_EXTENSION_EXTENSION }
            ?.mapNotNull {
                // Just in case, since Android 14+ requires them to be read-only
                if (it.canWrite()) {
                    it.setReadOnly()
                }

                val path = it.absolutePath
                pkgManager.getPackageArchiveInfo(path, PACKAGE_FLAGS)
                    ?.apply { applicationInfo!!.fixBasePaths(path) }
            }
            ?.filter { isPackageAnExtension(it) }
            ?.map { ExtensionInfo(packageInfo = it, isShared = false) }
            ?: emptySequence()

        val extPkgs = (sharedExtPkgs + privateExtPkgs)
            // Remove duplicates. Shared takes priority than private by default
            .distinctBy { it.packageInfo.packageName }
            // Compare version number
            .mapNotNull { sharedPkg ->
                val privatePkg = privateExtPkgs
                    .singleOrNull { it.packageInfo.packageName == sharedPkg.packageInfo.packageName }
                selectExtensionPackage(sharedPkg, privatePkg)
            }
            .toList()

        if (extPkgs.isEmpty()) return emptyList()

        // Load each extension concurrently and wait for completion
        return runBlocking {
            val deferred = extPkgs.map {
                async { loadExtension(context, it) }
            }
            deferred.awaitAll()
        }
    }

    /**
     * Attempts to load an extension from the given package name. It checks if the extension
     * contains the required feature flag before trying to load it.
     */
    suspend fun loadExtensionFromPkgName(context: Context, pkgName: String): LoadResult {
        val extensionPackage = getExtensionInfoFromPkgName(context, pkgName)
        if (extensionPackage == null) {
            logcat(LogPriority.ERROR) { "Extension package is not found ($pkgName)" }
            return LoadResult.Error
        }
        return loadExtension(context, extensionPackage)
    }

    fun getExtensionPackageInfoFromPkgName(context: Context, pkgName: String): PackageInfo? {
        return getExtensionInfoFromPkgName(context, pkgName)?.packageInfo
    }

    private fun getExtensionInfoFromPkgName(context: Context, pkgName: String): ExtensionInfo? {
        val privateExtensionFile = File(getPrivateExtensionDir(context), "$pkgName.$PRIVATE_EXTENSION_EXTENSION")
        val privatePkg = if (privateExtensionFile.isFile) {
            context.packageManager.getPackageArchiveInfo(privateExtensionFile.absolutePath, PACKAGE_FLAGS)
                ?.takeIf { isPackageAnExtension(it) }
                ?.let {
                    it.applicationInfo!!.fixBasePaths(privateExtensionFile.absolutePath)
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
                .takeIf { isPackageAnExtension(it) }
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
     * Loads an extension
     *
     * @param context The application context.
     * @param extensionInfo The extension to load.
     */
    private suspend fun loadExtension(context: Context, extensionInfo: ExtensionInfo): LoadResult {
        val pkgManager = context.packageManager
        val pkgInfo = extensionInfo.packageInfo
        val appInfo = pkgInfo.applicationInfo!!
        val pkgName = pkgInfo.packageName

        val extName = (appInfo.metaData.getString(METADATA_NAME) ?: pkgManager.getApplicationLabel(appInfo).toString())
            .removePrefix("Tachiyomi: ")
            .removePrefix("Katari: ")
            .removePrefix("K-Mihon: ")
        val versionName = pkgInfo.versionName
        val versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo)

        if (versionName.isNullOrEmpty()) {
            logcat(LogPriority.WARN) { "Missing versionName for extension $extName" }
            return LoadResult.Error
        }

        // Validate lib version
        @Suppress("DEPRECATION")
        val libVersionName = getExtensionLibVersion(appInfo.metaData.get(METADATA_EXTENSION_LIB))
            ?: versionName.substringBeforeLast('.')
        val libVersion = LibVersion.parse(libVersionName)
        if (libVersion == null || !isLibVersionNameCompatible(libVersionName)) {
            logcat(LogPriority.WARN) {
                "Lib version is $libVersionName, while only versions " +
                    "${supportedLibVersionRangesString()} are allowed"
            }
            return LoadResult.Error
        }

        val signatures = getSignatures(pkgInfo)
        if (signatures.isNullOrEmpty()) {
            logcat(LogPriority.WARN) { "Package $pkgName isn't signed" }
            return LoadResult.Error
        } else if (!trustExtension.isTrusted(pkgInfo, signatures)) {
            val extension = Extension.Untrusted(
                extName,
                pkgName,
                versionName,
                versionCode,
                libVersion.toDouble(),
                signatures.last(),
            )
            logcat(LogPriority.WARN) { "Extension $pkgName isn't trusted" }
            return LoadResult.Untrusted(extension)
        }

        val isNsfw = appInfo.metaData.getInt(METADATA_CONTENT_WARNING) > 0 ||
            appInfo.metaData.getInt(METADATA_NSFW) == 1
        if (!loadNsfwSource && isNsfw) {
            logcat(LogPriority.WARN) { "NSFW extension $pkgName not allowed" }
            return LoadResult.Error
        }

        val classLoader = try {
            createExtensionClassLoader(appInfo, context, libVersionName)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Extension load error: $extName ($pkgName)" }
            return LoadResult.Error
        }

        val sourceClasses = appInfo.metaData.getString(METADATA_SOURCE_CLASS)!!
            .split(";")
            .map {
                val sourceClass = it.trim()
                if (sourceClass.startsWith(".")) {
                    pkgInfo.packageName + sourceClass
                } else {
                    sourceClass
                }
            }

        val sources = sourceClasses.flatMap { sourceClass ->
            try {
                when (
                    val obj = Class.forName(
                        sourceClass,
                        false,
                        classLoader,
                    ).getDeclaredConstructor().newInstance()
                ) {
                    is UnifiedSource -> listOf(obj)
                    is EntrySourceFactory -> obj.createSources()
                    is Source -> listOf(obj.asUnifiedSource())
                    is SourceFactory -> obj.createSources().map { it.asUnifiedSource() }
                    else -> throw Exception("Unknown source class type: ${obj.javaClass}")
                }
            } catch (e: Throwable) {
                logcat(LogPriority.ERROR, e) { "Extension load error: $extName ($sourceClass)" }
                return LoadResult.Error
            }
        }

        val langs = sources.filterIsInstance<EntryCatalogueSource>()
            .map { it.lang }
            .toSet()
        val lang = when (langs.size) {
            0 -> ""
            1 -> langs.first()
            else -> "all"
        }

        val extension = Extension.Installed(
            name = extName,
            pkgName = pkgName,
            versionName = versionName,
            versionCode = versionCode,
            libVersion = libVersion.toDouble(),
            lang = lang,
            isNsfw = isNsfw,
            pkgFactory = appInfo.metaData.getString(METADATA_SOURCE_FACTORY),
            sources = sources,
            icon = appInfo.loadIcon(pkgManager),
            isShared = extensionInfo.isShared,
        )
        return LoadResult.Success(extension)
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
     * Returns true if the given package is an extension.
     *
     * @param pkgInfo The package info of the application.
     */
    private fun isPackageAnExtension(pkgInfo: PackageInfo): Boolean {
        return pkgInfo.reqFeatures.orEmpty().any { it.name == EXTENSION_FEATURE }
    }

    fun isLibVersionCompatible(versionName: String): Boolean {
        return parseVersionNameAsLibVersion(versionName)
            ?.let(::isLibVersionCompatible)
            ?: false
    }

    fun isRawLibVersionCompatible(versionName: String): Boolean {
        return LibVersion.parse(versionName)
            ?.let(::isLibVersionCompatible)
            ?: false
    }

    private fun isLibVersionNameCompatible(versionName: String): Boolean {
        return isRawLibVersionCompatible(versionName)
    }

    private fun isLibVersionCompatible(libVersion: LibVersion): Boolean {
        return supportedLibVersionRanges().any { libVersion in it } ||
            supportedEntrySourceApiFamilies().any { family ->
                libVersion.major == family.major && libVersion.minor == family.minor
            }
    }

    fun compareLibVersions(firstVersionName: String, secondVersionName: String): Int? {
        val first = parseVersionNameAsLibVersion(firstVersionName) ?: return null
        val second = parseVersionNameAsLibVersion(secondVersionName) ?: return null
        return first.compareTo(second)
    }

    private fun parseVersionNameAsLibVersion(versionName: String): LibVersion? {
        return LibVersion.parse(versionName.substringBeforeLast('.'))
            ?: LibVersion.parse(versionName)
    }

    private fun supportedLibVersionRanges(): List<ClosedRange<LibVersion>> {
        return UPSTREAM_SOURCE_API_VERSION_RANGES
    }

    private fun supportedEntrySourceApiFamilies(): List<LibVersion> {
        return ENTRY_SOURCE_API_VERSION_FAMILIES
    }

    private fun supportedLibVersionRangesString(): String {
        val ranges = supportedLibVersionRanges().map { range ->
            if (range.start == range.endInclusive) {
                range.start.toString()
            } else {
                "${range.start} to ${range.endInclusive}"
            }
        }
        val families = supportedEntrySourceApiFamilies().map { family ->
            "${family.major}.${family.minor}.*"
        }
        return (ranges + families).joinToString()
    }

    private fun libVersionRange(start: String, endInclusive: String): ClosedRange<LibVersion> {
        return LibVersion.parse(start)!!..LibVersion.parse(endInclusive)!!
    }

    internal fun getExtensionLibVersion(value: Any?): String? {
        return when (value) {
            is Float -> value.takeUnless { it == 0.0f }?.toString()
            is Double -> value.takeUnless { it == 0.0 }?.toString()
            is String -> value.takeUnless { it.isBlank() || it == "0" || it == "0.0" }
            else -> null
        }
    }

    private fun createExtensionClassLoader(
        appInfo: ApplicationInfo,
        context: Context,
        libVersionName: String,
    ): ClassLoader {
        if (shouldUseDelegateLastClassLoader(libVersionName, Build.VERSION.SDK_INT)) {
            try {
                // Installed APKs are optimized for PathClassLoader. Loading a private cache copy
                // prevents ART from reusing that incompatible context with DelegateLastClassLoader.
                val cachedExtension = cacheExtensionApk(
                    source = File(appInfo.sourceDir),
                    cacheDir = File(context.codeCacheDir, "extension_apks"),
                    packageName = appInfo.packageName,
                )
                return DelegateLastClassLoader(cachedExtension.absolutePath, null, context.classLoader)
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e) {
                    "Failed to prepare the optimized extension class loader for ${appInfo.packageName}"
                }
            }
        }

        return ChildFirstPathClassLoader(appInfo.sourceDir, null, context.classLoader)
    }

    internal fun cacheExtensionApk(source: File, cacheDir: File, packageName: String): File {
        val filePrefix = "$packageName-"
        val target = File(cacheDir, "$filePrefix${source.length()}-${source.lastModified()}.apk")

        if (!target.isFile || target.length() != source.length()) {
            source.copyAndSetReadOnlyTo(target, overwrite = true)
            cacheDir.listFiles()
                ?.filter { it.isFile && it != target && it.name.startsWith(filePrefix) }
                ?.forEach(File::delete)
        }

        return target
    }

    internal fun shouldUseDelegateLastClassLoader(libVersionName: String, sdkInt: Int): Boolean {
        if (sdkInt < Build.VERSION_CODES.O_MR1) return false

        val libVersion = parseVersionNameAsLibVersion(libVersionName) ?: return false
        return supportedLibVersionRanges().any { libVersion in it }
    }

    private data class LibVersion(
        val major: Int,
        val minor: Int,
        val patch: Int = 0,
    ) : Comparable<LibVersion> {

        override fun compareTo(other: LibVersion): Int {
            return compareValuesBy(this, other, LibVersion::major, LibVersion::minor, LibVersion::patch)
        }

        override fun toString(): String {
            return if (patch == 0) {
                "$major.$minor"
            } else {
                "$major.$minor.$patch"
            }
        }

        fun toDouble(): Double {
            return "$major.$minor".toDouble()
        }

        companion object {
            fun parse(value: String): LibVersion? {
                val parts = value.split('.')
                if (parts.size !in 2..3) return null

                return LibVersion(
                    major = parts[0].toIntOrNull() ?: return null,
                    minor = parts[1].toIntOrNull() ?: return null,
                    patch = parts.getOrNull(2)?.toIntOrNull() ?: 0,
                )
            }
        }
    }

    /**
     * Returns the signatures of the package or null if it's not signed.
     *
     * @param pkgInfo The package info of the application.
     * @return List SHA256 digest of the signatures
     */
    private fun getSignatures(pkgInfo: PackageInfo): List<String>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = pkgInfo.signingInfo!!
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
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
}
