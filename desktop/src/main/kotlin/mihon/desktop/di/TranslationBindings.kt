package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.IntoSet
import eu.kanade.tachiyomi.ui.entry.translation.EntryTranslateSettingsScreens
import mihon.desktop.translation.InProcessEntryTranslateWorkController
import mihon.desktop.translation.NoTranslationFeature
import mihon.desktop.translation.NoTranslationHostActions
import mihon.desktop.translation.WindowEntryTranslateSettingsScreens
import mihon.desktop.work.ResumableWork
import mihon.entry.interactions.translate.work.EntryTranslateWorkController
import mihon.translation.api.TranslationFeature
import mihon.translation.api.host.TranslationHostActions

@BindingContainer
interface TranslationBindings {
    @Binds
    val InProcessEntryTranslateWorkController.entryTranslateWorkController: EntryTranslateWorkController

    @Binds
    @IntoSet
    val InProcessEntryTranslateWorkController.resumableEntryTranslations: ResumableWork

    @Binds
    val WindowEntryTranslateSettingsScreens.entryTranslateSettingsScreens: EntryTranslateSettingsScreens

    @Binds
    val NoTranslationFeature.translationFeature: TranslationFeature

    @Binds
    val NoTranslationHostActions.translationHostActions: TranslationHostActions
}
