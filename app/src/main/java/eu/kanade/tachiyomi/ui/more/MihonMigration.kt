package eu.kanade.tachiyomi.ui.more

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

internal val MIHON_PACKAGE_NAMES = listOf(
    "app.mihon",
    "app.mihon.foss",
    "app.mihon.debug",
)

internal fun shouldOfferMihonMigration(
    onboardingComplete: Boolean,
    migrationPromptHandled: Boolean,
    mihonInstalled: Boolean,
): Boolean {
    return !onboardingComplete && !migrationPromptHandled && mihonInstalled
}

internal fun Context.findInstalledMihonPackage(): String? {
    return resolveInstalledMihonPackage { packageName ->
        try {
            getMihonApplicationInfo(packageName)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}

internal fun resolveInstalledMihonPackage(isInstalled: (String) -> Boolean): String? {
    return MIHON_PACKAGE_NAMES.firstOrNull(isInstalled)
}

private fun Context.getMihonApplicationInfo(packageName: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getApplicationInfo(
            packageName,
            PackageManager.ApplicationInfoFlags.of(0),
        )
    } else {
        @Suppress("DEPRECATION")
        packageManager.getApplicationInfo(packageName, 0)
    }
}
