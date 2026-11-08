package mihon.translation.api.host

import android.content.Context
import android.content.Intent

private const val SETTINGS_ACTIVITY_CLASS_NAME = "eu.kanade.tachiyomi.ui.setting.SettingsActivity"

object TranslationSettingsNavigation {
    const val ACTION_OPEN_SETTINGS = "mihon.translation.action.OPEN_SETTINGS"
}

fun Context.openTranslationSettings() {
    startActivity(
        Intent(TranslationSettingsNavigation.ACTION_OPEN_SETTINGS)
            .setClassName(packageName, SETTINGS_ACTIVITY_CLASS_NAME),
    )
}
