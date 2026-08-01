package mihon.entry.viewer.settings.navigation

import android.content.Context
import android.content.Intent

object ViewerSettingsNavigation {
    const val ACTION_OPEN_SETTINGS = "mihon.entry.viewer.settings.action.OPEN_SETTINGS"
    const val EXTRA_SURFACE_ID = "mihon.entry.viewer.settings.extra.SURFACE_ID"
}

fun Context.openViewerSettings(surfaceId: String) {
    require(surfaceId.isNotBlank()) { "Viewer settings surface ID must not be blank" }
    packageManager
        .getLaunchIntentForPackage(packageName)
        ?.apply {
            action = ViewerSettingsNavigation.ACTION_OPEN_SETTINGS
            putExtra(ViewerSettingsNavigation.EXTRA_SURFACE_ID, surfaceId)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        ?.let(::startActivity)
}
