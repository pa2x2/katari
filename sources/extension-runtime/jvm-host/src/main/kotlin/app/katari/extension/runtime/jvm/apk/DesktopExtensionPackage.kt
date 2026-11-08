package app.katari.extension.runtime.jvm.apk

import app.katari.extension.runtime.jvm.apk.binary.ResourceValue
import eu.kanade.tachiyomi.extension.platform.ExtensionMetaData
import eu.kanade.tachiyomi.extension.platform.ExtensionPackage
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile

/**
 * An apk file read as a package manager reads an archive: its manifest at once, its signers verified, and its
 * resources, which only the label and the icon need, when first asked for. Desktop installs nothing for every app, so
 * it's never [isShared].
 */
class DesktopExtensionPackage internal constructor(
    val apk: File,
    internal val manifest: ApkManifest,
    override val signatures: List<String>?,
) : ExtensionPackage {

    override val packageName: String
        get() = manifest.packageName

    override val versionName: String?
        get() = manifest.versionName

    override val versionCode: Long
        get() = manifest.versionCode

    override val features: Set<String>
        get() = manifest.features

    override val metaData: ExtensionMetaData? by lazy {
        manifest.metaData?.let { entries -> ApkMetaData.of(entries) { resources } }
    }

    override val isShared: Boolean = false

    /** The SHA-256 of the whole file, which names the jar its code is converted to. */
    val sha256: String by lazy {
        val digest = MessageDigest.getInstance("SHA-256")
        apk.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }

    internal val resources: ApkResources? by lazy {
        ZipFile(apk).use { zip -> zip.getEntry(RESOURCE_TABLE)?.let { zip.getInputStream(it).readBytes() } }
            ?.let(::ApkResources)
    }

    /**
     * As Android's `PackageItemInfo.loadLabel`: the label written in the manifest as it is, or its string resource
     * trimmed, or else the application class's name, or the package's.
     */
    override fun label(): String {
        val label = manifest.label
        label?.string?.let { return it }
        if (label?.value?.type == ResourceValue.REFERENCE) {
            resources?.string(label.value.data)?.let { return it.trim() }
        }
        return manifest.applicationName ?: packageName
    }

    internal companion object {
        const val MANIFEST = "AndroidManifest.xml"
        private const val RESOURCE_TABLE = "resources.arsc"
    }
}
