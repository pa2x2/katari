package eu.kanade.tachiyomi.extension.platform

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import eu.kanade.tachiyomi.util.lang.Hash

/** A package as [PackageManager] read it, with the flags [AndroidExtensionPackages] reads every package with. */
class AndroidExtensionPackage(
    val packageInfo: PackageInfo,
    override val isShared: Boolean,
    private val packageManager: PackageManager,
) : ExtensionPackage {

    override val packageName: String
        get() = packageInfo.packageName

    override val versionName: String?
        get() = packageInfo.versionName

    override val versionCode: Long
        get() = PackageInfoCompat.getLongVersionCode(packageInfo)

    override val features: Set<String>
        get() = packageInfo.reqFeatures.orEmpty().mapNotNullTo(HashSet()) { it.name }

    override val metaData: ExtensionMetaData?
        get() = packageInfo.applicationInfo?.metaData?.let(::AndroidExtensionMetaData)

    override val signatures: List<String>?
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo = packageInfo.signingInfo
                when {
                    signingInfo == null -> null
                    signingInfo.hasMultipleSigners() -> signingInfo.apkContentsSigners
                    else -> signingInfo.signingCertificateHistory
                }
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
                ?.map { Hash.sha256(it.toByteArray()) }
                ?.toList()
        }

    override fun label(): String =
        packageInfo.applicationInfo?.let { packageManager.getApplicationLabel(it).toString() } ?: packageName
}
