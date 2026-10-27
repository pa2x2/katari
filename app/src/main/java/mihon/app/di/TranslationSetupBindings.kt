package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.tachiyomi.ui.entry.translation.AndroidEntryTranslateSetupHost
import eu.kanade.tachiyomi.ui.entry.translation.EntryTranslateSetupHost

@BindingContainer
interface TranslationSetupBindings {
    @Binds
    val AndroidEntryTranslateSetupHost.entryTranslateSetupHost: EntryTranslateSetupHost
}
