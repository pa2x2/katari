package app.katari.android.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.tachiyomi.ui.entry.cover.AndroidEntryCoverDocuments
import eu.kanade.tachiyomi.ui.entry.cover.AndroidEntryCoverExport
import eu.kanade.tachiyomi.ui.entry.cover.EntryCoverDocuments
import eu.kanade.tachiyomi.ui.entry.cover.EntryCoverExport

@BindingContainer
interface EntryScreenBindings {
    @Binds
    val AndroidEntryCoverExport.entryCoverExport: EntryCoverExport

    @Binds
    val AndroidEntryCoverDocuments.entryCoverDocuments: EntryCoverDocuments
}
