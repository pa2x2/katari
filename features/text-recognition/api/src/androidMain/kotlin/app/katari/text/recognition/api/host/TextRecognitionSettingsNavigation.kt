package app.katari.text.recognition.api.host

import android.content.Context
import android.content.Intent
import app.katari.language.api.tag.LanguageTag

private const val SETTINGS_ACTIVITY_CLASS_NAME = "eu.kanade.tachiyomi.ui.setting.SettingsActivity"

object TextRecognitionSettingsNavigation {
    /** Opens the pipeline choice for [EXTRA_LANGUAGE]; the choice is stored as soon as it is confirmed. */
    const val ACTION_CHOOSE_PIPELINE = "app.katari.text.recognition.action.CHOOSE_PIPELINE"
    const val EXTRA_LANGUAGE = "app.katari.text.recognition.extra.LANGUAGE"
}

/** Lets the user choose how [language] is read and returns once the choice is stored. */
fun Context.openTextRecognitionPipelineChoice(language: LanguageTag) {
    startActivity(
        Intent(TextRecognitionSettingsNavigation.ACTION_CHOOSE_PIPELINE)
            .setClassName(packageName, SETTINGS_ACTIVITY_CLASS_NAME)
            .putExtra(TextRecognitionSettingsNavigation.EXTRA_LANGUAGE, language.value),
    )
}
