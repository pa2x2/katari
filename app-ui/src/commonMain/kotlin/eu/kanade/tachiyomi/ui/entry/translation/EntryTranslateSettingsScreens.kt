package eu.kanade.tachiyomi.ui.entry.translation

import app.katari.core.common.platform.PlatformFacility
import app.katari.core.platform.PlatformContext
import app.katari.language.api.tag.LanguageTag
import app.katari.translation.api.host.TranslationSettingsDestination

/**
 * The settings screens the translation setup sheet sends the user to, opened the way the platform shows a screen from
 * [PlatformContext], the screen the sheet is over.
 */
@PlatformFacility
interface EntryTranslateSettingsScreens : TranslationSettingsDestination {
    /** Lets the user choose how [language] is read, and returns to the sheet once the choice is stored. */
    fun openPipelineChoice(context: PlatformContext, language: LanguageTag)
}
