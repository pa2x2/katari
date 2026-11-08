package app.katari.android.di

import app.katari.translation.api.host.TranslationSettingsDestination
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.tachiyomi.ui.entry.translation.AndroidEntryTranslateSettingsScreens
import eu.kanade.tachiyomi.ui.entry.translation.EntryTranslateSettingsScreens

@BindingContainer
interface TranslationSetupBindings {
    @Binds
    val AndroidEntryTranslateSettingsScreens.entryTranslateSettingsScreens: EntryTranslateSettingsScreens

    @Binds
    val AndroidEntryTranslateSettingsScreens.translationSettingsDestination: TranslationSettingsDestination
}
