package mihon.video.engine.mpv.addon

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import mihon.video.engine.mpv.loading.nativeAbi
import java.io.File

/** The installed add-on package as Katari judges it, read once per check. */
internal class InstalledMpvAddon(
    val versionName: String,
    val versionCode: Long,
    val glue: Int?,
    val nativeLibraryDir: File,
    /** The add-on's own libraries for [MpvAddonRequirement.abi], in load order; null when it has none or no list. */
    val libraries: List<File>?,
    val signedByKatari: Boolean,
    val abi: String?,
    val sizeBytes: Long,
) {
    /** Why Katari can't load this add-on, or null when it can. */
    fun problem(requirement: MpvAddonRequirement): MpvAddonProblem? = when {
        !signedByKatari -> MpvAddonProblem.ForeignSigner
        !requirement.acceptsGlue(glue) -> MpvAddonProblem.GlueMismatch
        !requirement.acceptsVersion(versionCode) -> MpvAddonProblem.TooOld
        abi != requirement.abi -> MpvAddonProblem.WrongAbi
        libraries == null -> MpvAddonProblem.Damaged
        else -> null
    }

    companion object {
        /** Null when the add-on isn't installed. */
        fun read(context: Context, requirement: MpvAddonRequirement): InstalledMpvAddon? {
            val packageManager = context.packageManager
            val info = try {
                packageManager.packageInfo(MpvAddonPackage.NAME, PackageManager.GET_META_DATA)
            } catch (_: PackageManager.NameNotFoundException) {
                return null
            }
            val application = info.applicationInfo ?: return null
            val libraryDir = File(application.nativeLibraryDir)
            return InstalledMpvAddon(
                versionName = info.versionName.orEmpty(),
                versionCode = PackageInfoCompat.getLongVersionCode(info),
                glue = application.metaData?.takeIf { it.containsKey(MpvAddonPackage.GLUE_META_DATA) }
                    ?.getInt(MpvAddonPackage.GLUE_META_DATA),
                nativeLibraryDir = libraryDir,
                libraries = readLibraries(context, libraryDir, requirement.abi),
                signedByKatari = isSignedByKatari(context),
                abi = application.nativeAbi(),
                sizeBytes = application.sizeOnDisk(),
            )
        }

        /**
         * The libraries in the order of the add-on's own `load-order.json`, read without loading its code. A newer
         * add-on may ship another set than the libmpv release Katari was built with.
         */
        private fun readLibraries(context: Context, libraryDir: File, abi: String): List<File>? {
            val loadOrder = try {
                context.createPackageContext(MpvAddonPackage.NAME, 0).assets.open(MpvLoadOrder.ASSET)
                    .bufferedReader()
                    .use { MpvLoadOrder.parse(it.readText()) }
            } catch (_: Exception) {
                null
            }
            return loadOrder?.loadOrder?.get(abi)
                ?.map { File(libraryDir, it) }
                ?.takeIf { libraries -> libraries.isNotEmpty() && libraries.all(File::isFile) }
        }

        /** Signed with one of Katari's own certificates, so it comes from the same builder as Katari itself. */
        private fun isSignedByKatari(context: Context): Boolean {
            val packageManager = context.packageManager
            val own = packageManager.packageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                .signingInfo?.apkContentsSigners.orEmpty()
            return own.any {
                packageManager.hasSigningCertificate(
                    MpvAddonPackage.NAME,
                    it.toByteArray(),
                    PackageManager.CERT_INPUT_RAW_X509,
                )
            }
        }

        private fun ApplicationInfo.sizeOnDisk(): Long =
            File(sourceDir).length() + (File(nativeLibraryDir).listFiles()?.sumOf { it.length() } ?: 0L)

        private fun PackageManager.packageInfo(packageName: String, flags: Int): PackageInfo =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                getPackageInfo(packageName, flags)
            }
    }
}
