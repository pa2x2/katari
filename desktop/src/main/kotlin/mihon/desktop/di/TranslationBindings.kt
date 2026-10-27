package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.tachiyomi.ui.entry.translation.EntryTranslateSetupHost
import mihon.desktop.translation.NoEntryTranslateFeature
import mihon.desktop.translation.NoEntryTranslateSetupHost
import mihon.desktop.translation.NoEntryTranslationLanguagesFeature
import mihon.entry.interactions.translate.EntryTranslateFeature
import mihon.entry.interactions.translation.EntryTranslationLanguagesFeature

@BindingContainer
interface TranslationBindings {
    @Binds
    val NoEntryTranslateFeature.entryTranslateFeature: EntryTranslateFeature

    @Binds
    val NoEntryTranslationLanguagesFeature.entryTranslationLanguagesFeature: EntryTranslationLanguagesFeature

    @Binds
    val NoEntryTranslateSetupHost.entryTranslateSetupHost: EntryTranslateSetupHost
}
