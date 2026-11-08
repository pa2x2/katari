package app.katari.desktop.di

import app.katari.desktop.entry.FileEntryCoverDocuments
import app.katari.desktop.entry.FileEntryCoverExport
import app.katari.desktop.source.PlatformGapErrorExplanation
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.IntoSet
import eu.kanade.presentation.util.SourceErrorExplanation
import eu.kanade.tachiyomi.ui.entry.cover.EntryCoverDocuments
import eu.kanade.tachiyomi.ui.entry.cover.EntryCoverExport

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
