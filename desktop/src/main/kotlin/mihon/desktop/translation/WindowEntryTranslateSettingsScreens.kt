package mihon.desktop.translation

import cafe.adriel.voyager.core.screen.Screen
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.textrecognition.languages.TextRecognitionOverridePipelineScreen
import eu.kanade.tachiyomi.ui.entry.translation.EntryTranslateSettingsScreens
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import mihon.core.platform.PlatformContext
import mihon.desktop.window.WindowPlatformContext
import mihon.language.api.tag.LanguageTag

/**
 * Opens the settings screens on the navigator of the window the call comes from, over the screen the sheet is on, so
 * leaving them returns to it. A call from no window opens nothing, as there is nowhere to show the screen.
 */
@Inject
class WindowEntryTranslateSettingsScreens : EntryTranslateSettingsScreens {
    override fun openPipelineChoice(context: PlatformContext, language: LanguageTag) =
        context.push(TextRecognitionOverridePipelineScreen(language.value, storeOnConfirm = true))

    override fun openTranslationSettings(context: PlatformContext) =
        context.push(SettingsScreen(SettingsScreen.Destination.Translation))

    private fun PlatformContext.push(screen: Screen) {
        (this as? WindowPlatformContext)?.navigator?.push(screen)
    }
}
