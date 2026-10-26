package eu.kanade.tachiyomi.extension.file

import kotlinx.serialization.Serializable
import mihon.domain.extension.model.ContentWarning
import java.io.File

/** A file extension as installed: its [manifest] and the folder of the version it names. */
internal class InstalledFileExtension(
    val kind: FileExtensionKind,
    val manifest: FileExtensionManifest,
    /** The installed version's folder: [code], [icon] and the companions its store listed, by name. */
    val contentDir: File,
) {
    val pkgName: String get() = fileExtensionPkgName(kind, manifest.id)

    val code: File get() = File(contentDir, CODE_FILE)

    val icon: File get() = File(contentDir, ICON_FILE)

    fun companion(name: String): File? = File(contentDir, name).takeIf { name in manifest.companions && it.isFile }

    companion object {
        const val CODE_FILE = "code"
        const val ICON_FILE = "icon"
    }
}

/**
 * What a store listed for the installed version, written once its files are in place. Writing it is what commits an
 * install or update.
 */
@Serializable
internal data class FileExtensionManifest(
    /** The extension's own ID among its kind's. */
    val id: String,
    val name: String,
    val lang: String,
    val versionName: String,
    val libVersionName: String,
    val contentWarning: ContentWarning,
    val storeIndexUrl: String,
    val fileUrl: String,
    val iconUrl: String,
    /** Of the code file, which also names the version's folder. */
    val sha256: String,
    /** Files the store listed beside the code, by the name the kind reads them under. */
    val companions: List<String> = emptyList(),
)

internal fun fileExtensionPkgName(kind: FileExtensionKind, id: String): String = "${kind.directoryName}:$id"
