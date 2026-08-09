package mihon.entry.viewer.settings.navigation

import android.content.Context
import android.content.Intent

private const val SETTINGS_ACTIVITY_CLASS_NAME = "eu.kanade.tachiyomi.ui.setting.SettingsActivity"

object ViewerSettingsNavigation {
    const val ACTION_OPEN_SETTINGS = "mihon.entry.viewer.settings.action.OPEN_SETTINGS"
    const val EXTRA_SURFACE_ID = "mihon.entry.viewer.settings.extra.SURFACE_ID"
}

fun Context.openViewerSettings(surfaceId: String) {
    require(surfaceId.isNotBlank()) { "Viewer settings surface ID must not be blank" }
    startActivity(
        Intent(ViewerSettingsNavigation.ACTION_OPEN_SETTINGS)
            .setClassName(packageName, SETTINGS_ACTIVITY_CLASS_NAME)
            .putExtra(ViewerSettingsNavigation.EXTRA_SURFACE_ID, surfaceId),
    )
}
