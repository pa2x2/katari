package mihon.entry.viewer.settings.navigation

import android.content.Context
import android.content.Intent

private const val SETTINGS_ACTIVITY_CLASS_NAME = "eu.kanade.tachiyomi.ui.setting.SettingsActivity"

object ViewerSettingsNavigation {
    const val ACTION_OPEN_SETTINGS = "mihon.entry.viewer.settings.action.OPEN_SETTINGS"
    const val EXTRA_SURFACE_ID = "mihon.entry.viewer.settings.extra.SURFACE_ID"

    /** The title of the row to scroll to and pulse, as settings search does. */
    const val EXTRA_HIGHLIGHT = "mihon.entry.viewer.settings.extra.HIGHLIGHT"
}

/** Opens [surfaceId]'s settings, scrolled to the row titled [highlight] when it is given. */
fun Context.openViewerSettings(surfaceId: String, highlight: String? = null) {
    require(surfaceId.isNotBlank()) { "Viewer settings surface ID must not be blank" }
    startActivity(
        Intent(ViewerSettingsNavigation.ACTION_OPEN_SETTINGS)
            .setClassName(packageName, SETTINGS_ACTIVITY_CLASS_NAME)
            .putExtra(ViewerSettingsNavigation.EXTRA_SURFACE_ID, surfaceId)
            .putExtra(ViewerSettingsNavigation.EXTRA_HIGHLIGHT, highlight),
    )
}
