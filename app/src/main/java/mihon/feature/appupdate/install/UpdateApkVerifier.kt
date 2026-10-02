package mihon.feature.appupdate.install

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import mihon.feature.appupdate.AppUpdateError
import mihon.feature.appupdate.AppUpdateException
import tachiyomi.core.common.util.lang.withIOContext
import java.io.File
import java.security.MessageDigest

/**
 * Checks a downloaded APK before it is handed to Android, so a wrong file fails with a reason the user can act on
 * instead of Android's "App not installed".
 */
internal class UpdateApkVerifier(private val context: Context) {

    /**
     * Throws [AppUpdateException] unless [file] matches [sha256] (when GitHub has a digest) and is a newer build of
     * this app signed with the same key.
     */
    suspend fun verify(file: File, sha256: String?) = withIOContext {
        if (!file.isFile) throw AppUpdateException(AppUpdateError.Damaged)
        if (sha256 != null && !sha256Of(file).equals(sha256, ignoreCase = true)) {
            throw AppUpdateException(AppUpdateError.Damaged)
        }

        val archive = archiveInfo(file) ?: throw AppUpdateException(AppUpdateError.Damaged)
        val installed = installedInfo()
        if (archive.packageName != context.packageName ||
            PackageInfoCompat.getLongVersionCode(archive) <= PackageInfoCompat.getLongVersionCode(installed)
        ) {
            throw AppUpdateException(AppUpdateError.NotNewer)
        }
        if (!sameSigner(archive, installed)) throw AppUpdateException(AppUpdateError.SignatureMismatch)
    }

    private fun archiveInfo(file: File): PackageInfo? {
        val packageManager = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageArchiveInfo(file.path, PackageManager.PackageInfoFlags.of(SIGNING_FLAGS.toLong()))
        } else {
            packageManager.getPackageArchiveInfo(file.path, SIGNING_FLAGS)
        }
    }

    private fun installedInfo(): PackageInfo {
        val packageManager = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(SIGNING_FLAGS.toLong()),
            )
        } else {
            packageManager.getPackageInfo(context.packageName, SIGNING_FLAGS)
        }
    }

    /** The update may be signed by a rotated key, whose history includes the installed app's signer. */
    private fun sameSigner(archive: PackageInfo, installed: PackageInfo): Boolean {
        val current = installed.signingInfo?.apkContentsSigners ?: return false
        val incoming = archive.signingInfo ?: return false
        val accepted = if (incoming.hasMultipleSigners()) {
            incoming.apkContentsSigners
        } else {
            incoming.signingCertificateHistory
        }
        return current.any { signer -> accepted.orEmpty().any { it == signer } }
    }

    private fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 8)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val SIGNING_FLAGS = PackageManager.GET_SIGNING_CERTIFICATES
    }
}
