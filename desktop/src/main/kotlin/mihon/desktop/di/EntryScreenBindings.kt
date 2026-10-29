package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.util.SourceErrorMessages
import eu.kanade.tachiyomi.ui.entry.cover.EntryCoverDocuments
import eu.kanade.tachiyomi.ui.entry.cover.EntryCoverExport
import mihon.desktop.entry.FileEntryCoverDocuments
import mihon.desktop.entry.FileEntryCoverExport
import mihon.desktop.source.DesktopSourceErrorMessages

@BindingContainer
interface EntryScreenBindings {
    @Binds
    val FileEntryCoverExport.entryCoverExport: EntryCoverExport

    @Binds
    val FileEntryCoverDocuments.entryCoverDocuments: EntryCoverDocuments

    @Binds
    val DesktopSourceErrorMessages.sourceErrorMessages: SourceErrorMessages
}
