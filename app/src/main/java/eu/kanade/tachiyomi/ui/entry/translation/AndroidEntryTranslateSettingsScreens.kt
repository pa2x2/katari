package eu.kanade.tachiyomi.ui.entry.translation

import app.katari.core.platform.PlatformContext
import app.katari.language.api.tag.LanguageTag
import app.katari.text.recognition.api.host.openTextRecognitionPipelineChoice
import app.katari.translation.api.host.openTranslationSettings
import dev.zacsweers.metro.Inject

/** Opens the settings screens in the settings activity, over the screen the call comes from. */
@Inject
class AndroidEntryTranslateSettingsScreens : EntryTranslateSettingsScreens {
    override fun openPipelineChoice(context: PlatformContext, language: LanguageTag) =
        context.openTextRecognitionPipelineChoice(language)

    override fun openTranslationSettings(context: PlatformContext) = context.openTranslationSettings()
}
