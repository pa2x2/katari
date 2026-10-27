package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.tachiyomi.ui.entry.translation.EntryTranslateSetupHost
import mihon.desktop.translation.NoEntryTranslateFeature
import mihon.desktop.translation.NoEntryTranslateSetupHost
import mihon.entry.interactions.translate.EntryTranslateFeature

@BindingContainer
interface TranslationBindings {
    @Binds
    val NoEntryTranslateFeature.entryTranslateFeature: EntryTranslateFeature

    @Binds
    val NoEntryTranslateSetupHost.entryTranslateSetupHost: EntryTranslateSetupHost
}
