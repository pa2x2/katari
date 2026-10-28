package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.tachiyomi.ui.entry.translation.EntryTranslateSetupHost
import mihon.desktop.translation.NoEntryTranslateFeature
import mihon.desktop.translation.NoEntryTranslateSetupHost
import mihon.desktop.translation.NoTranslationFeature
import mihon.desktop.translation.NoTranslationHostActions
import mihon.entry.interactions.translate.EntryTranslateFeature
import mihon.translation.api.TranslationFeature
import mihon.translation.api.host.TranslationHostActions

@BindingContainer
interface TranslationBindings {
    @Binds
    val NoEntryTranslateFeature.entryTranslateFeature: EntryTranslateFeature

    @Binds
    val NoEntryTranslateSetupHost.entryTranslateSetupHost: EntryTranslateSetupHost

    @Binds
    val NoTranslationFeature.translationFeature: TranslationFeature

    @Binds
    val NoTranslationHostActions.translationHostActions: TranslationHostActions
}
