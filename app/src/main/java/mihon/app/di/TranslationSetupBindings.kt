package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.tachiyomi.ui.entry.translation.AndroidEntryTranslateSettingsScreens
import eu.kanade.tachiyomi.ui.entry.translation.EntryTranslateSettingsScreens
import mihon.translation.api.host.TranslationSettingsDestination

@BindingContainer
interface TranslationSetupBindings {
    @Binds
    val AndroidEntryTranslateSettingsScreens.entryTranslateSettingsScreens: EntryTranslateSettingsScreens

    @Binds
    val AndroidEntryTranslateSettingsScreens.translationSettingsDestination: TranslationSettingsDestination
}
