package eu.kanade.tachiyomi.ui.entry.translation

import dev.zacsweers.metro.Inject
import mihon.core.platform.PlatformContext
import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.host.openTextRecognitionPipelineChoice
import mihon.translation.api.host.openTranslationSettings

/** Opens the settings screens in the settings activity, over the screen the call comes from. */
@Inject
class AndroidEntryTranslateSettingsScreens : EntryTranslateSettingsScreens {
    override fun openPipelineChoice(context: PlatformContext, language: LanguageTag) =
        context.openTextRecognitionPipelineChoice(language)

    override fun openTranslationSettings(context: PlatformContext) = context.openTranslationSettings()
}
