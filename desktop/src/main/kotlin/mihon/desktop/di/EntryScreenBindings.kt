package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.IntoSet
import eu.kanade.presentation.util.SourceErrorExplanation
import eu.kanade.tachiyomi.ui.entry.cover.EntryCoverDocuments
import eu.kanade.tachiyomi.ui.entry.cover.EntryCoverExport
import mihon.desktop.entry.FileEntryCoverDocuments
import mihon.desktop.entry.FileEntryCoverExport
import mihon.desktop.source.PlatformGapErrorExplanation

@BindingContainer
interface EntryScreenBindings {
    @Binds
    val FileEntryCoverExport.entryCoverExport: EntryCoverExport

    @Binds
    val FileEntryCoverDocuments.entryCoverDocuments: EntryCoverDocuments

    @Binds
    @IntoSet
    val PlatformGapErrorExplanation.sourceErrorExplanation: SourceErrorExplanation
}
