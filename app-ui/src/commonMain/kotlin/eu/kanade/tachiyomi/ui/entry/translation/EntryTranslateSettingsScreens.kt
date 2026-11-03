package eu.kanade.tachiyomi.ui.entry.translation

import mihon.core.common.platform.PlatformFacility
import mihon.core.platform.PlatformContext
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.host.TranslationSettingsDestination

/**
 * The settings screens the translation setup sheet sends the user to, opened the way the platform shows a screen from
 * [PlatformContext], the screen the sheet is over.
 */
@PlatformFacility
interface EntryTranslateSettingsScreens : TranslationSettingsDestination {
    /** Lets the user choose how [language] is read, and returns to the sheet once the choice is stored. */
    fun openPipelineChoice(context: PlatformContext, language: LanguageTag)
}
