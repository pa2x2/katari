package eu.kanade.tachiyomi.extension.file

import android.content.Context
import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.tachiyomi.extension.kind.ExtensionKinds
import eu.kanade.tachiyomi.extension.kind.LibVersion
import eu.kanade.tachiyomi.extension.kind.LoadPassSourceKeys
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstalledForm
import eu.kanade.tachiyomi.extension.util.ExtensionLoadPolicy
import eu.kanade.tachiyomi.extension.util.rootMessage
import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.util.lang.Hash
import logcat.LogPriority
import mihon.domain.extension.model.ContentWarning
import tachiyomi.core.common.util.system.logcat
import kotlin.coroutines.cancellation.CancellationException

/**
 * Loads the extensions of [ExtensionKinds.fileKinds] from [FileExtensionStorage], with the checks every extension gets.
 * In place of a signature, the code must still hash to what was installed, and the store it came from must still be
 * added or the user must have trusted that hash.
 */
internal class FileExtensionLoader(
    private val context: Context,
    private val storage: FileExtensionStorage,
    private val policy: ExtensionLoadPolicy,
    private val trustExtension: TrustExtension,
    private val keys: LoadPassSourceKeys,
) {

    fun listInstalled(): List<StoredFileExtension> = ExtensionKinds.fileKinds.flatMap(storage::listInstalled)

    suspend fun load(stored: StoredFileExtension, alreadyLoaded: Extension.Loaded? = null): Extension.Installed {
        return when (stored) {
            is StoredFileExtension.Unreadable -> unreadable(stored)
            is StoredFileExtension.Readable -> try {
                load(stored.extension, alreadyLoaded)
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                logcat(LogPriority.ERROR, e) { "Extension load error: ${stored.extension.pkgName}" }
                stored.extension.notLoaded(Extension.NotLoaded.Reason.Failed(e.rootMessage, e.stackTraceToString()))
            }
        }
    }

    private suspend fun load(extension: InstalledFileExtension, alreadyLoaded: Extension.Loaded?): Extension.Installed {
        val kind = extension.kind
        val manifest = extension.manifest
        val pkgName = extension.pkgName

        if (!kind.runsLib(manifest.libVersionName)) {
            logcat(LogPriority.WARN) { "Extension $pkgName isn't loaded: lib ${manifest.libVersionName}" }
            return extension.notLoaded(
                Extension.NotLoaded.Reason.UnsupportedLibVersion(manifest.libVersionName, kind.providerKind),
            )
        }

        val code = extension.code
        if (!code.isFile || Hash.sha256(code.readBytes()) != manifest.sha256) {
            logcat(LogPriority.WARN) { "Extension $pkgName isn't the file that was installed" }
            return extension.notLoaded(Extension.NotLoaded.Reason.Damaged)
        }

        if (!trustExtension.isTrusted(pkgName, manifest.sha256, manifest.storeIndexUrl, policy.stores)) {
            logcat(LogPriority.WARN) { "Extension $pkgName isn't trusted" }
            return extension.notLoaded(Extension.NotLoaded.Reason.Untrusted(manifest.sha256))
        }

        if (policy.filters(manifest.contentWarning)) {
            logcat(LogPriority.WARN) { "Extension $pkgName with ${manifest.contentWarning} not allowed" }
            return extension.notLoaded(Extension.NotLoaded.Reason.Filtered)
        }

        // As for an apk: the same file still passing every check keeps the sources already registered for it
        if (alreadyLoaded != null && alreadyLoaded.form == extension.form) {
            return alreadyLoaded
        }

        val sources = try {
            kind.createSources(context, extension, keys)
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            logcat(LogPriority.ERROR, e) { "Extension load error: ${manifest.name} ($pkgName)" }
            return extension.notLoaded(Extension.NotLoaded.Reason.Failed(e.rootMessage, e.stackTraceToString()))
        }

        return Extension.Loaded(
            name = manifest.name,
            pkgName = pkgName,
            versionName = manifest.versionName,
            versionCode = 0,
            // The kind runs it, so the version parses
            libVersion = checkNotNull(LibVersion.parse(manifest.libVersionName)).toDouble(),
            lang = sources.filterIsInstance<EntryCatalogueSource>().map { it.lang }.distinct().singleOrNull()
                ?: manifest.lang,
            contentWarning = manifest.contentWarning,
            pkgFactory = null,
            sources = sources,
            sourceIdScheme = kind.sourceIdScheme,
            providerKind = kind.providerKind,
            form = extension.form,
        )
    }

    private val InstalledFileExtension.form get() = InstalledForm.File(manifest.sha256, manifest.storeIndexUrl)

    private fun InstalledFileExtension.notLoaded(reason: Extension.NotLoaded.Reason) = Extension.NotLoaded(
        name = manifest.name,
        pkgName = pkgName,
        versionName = manifest.versionName,
        versionCode = 0,
        form = form,
        contentWarning = manifest.contentWarning,
        libVersion = LibVersion.parse(manifest.libVersionName)?.toDouble(),
        lang = manifest.lang,
        reason = reason,
    )

    /** Uninstalling it deletes the folder it's listed by. */
    private fun unreadable(stored: StoredFileExtension.Unreadable): Extension.NotLoaded {
        val pkgName = fileExtensionPkgName(stored.kind, stored.dirName)
        return Extension.NotLoaded(
            name = pkgName,
            pkgName = pkgName,
            versionName = "",
            versionCode = 0,
            form = InstalledForm.File(sha256 = "", storeIndexUrl = ""),
            contentWarning = ContentWarning.SAFE,
            reason = Extension.NotLoaded.Reason.Unreadable,
        )
    }
}
