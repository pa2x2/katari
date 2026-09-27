package mihon.text.recognition.api.host

import android.content.Context
import android.content.Intent

private const val SETTINGS_ACTIVITY_CLASS_NAME = "eu.kanade.tachiyomi.ui.setting.SettingsActivity"

object TextRecognitionSettingsNavigation {
    const val ACTION_OPEN_SETTINGS = "mihon.text.recognition.action.OPEN_SETTINGS"
}

fun Context.openTextRecognitionSettings() {
    startActivity(
        Intent(TextRecognitionSettingsNavigation.ACTION_OPEN_SETTINGS)
            .setClassName(packageName, SETTINGS_ACTIVITY_CLASS_NAME),
    )
}
