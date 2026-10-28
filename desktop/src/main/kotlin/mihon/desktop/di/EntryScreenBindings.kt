package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.presentation.util.SourceErrorMessages
import eu.kanade.tachiyomi.data.entry.AppEntryScanlatorFilterDataSource
import eu.kanade.tachiyomi.ui.entry.cover.EntryCoverDocuments
import eu.kanade.tachiyomi.ui.entry.cover.EntryCoverExport
import eu.kanade.tachiyomi.ui.entry.missingsource.MissingSourceExtensions
import eu.kanade.tachiyomi.ui.entry.updates.EntryUpdateIntervalAvailability
import mihon.desktop.entry.FileEntryCoverDocuments
import mihon.desktop.entry.FileEntryCoverExport
import mihon.desktop.extension.NoMissingSourceExtensions
import mihon.desktop.source.NoSourceErrorMessages
import mihon.entry.interactions.runtime.EntryScanlatorFilterDataSource
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.entry.repository.EntryChapterRepository

@BindingContainer
interface EntryScreenBindings {
    @Binds
    val FileEntryCoverExport.entryCoverExport: EntryCoverExport

    @Binds
    val FileEntryCoverDocuments.entryCoverDocuments: EntryCoverDocuments

    @Binds
    val NoSourceErrorMessages.sourceErrorMessages: SourceErrorMessages

    @Binds
    val NoMissingSourceExtensions.missingSourceExtensions: MissingSourceExtensions

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun scanlatorFilterDataSource(
            entryChapterRepository: EntryChapterRepository,
            handler: DatabaseHandler,
            profileProvider: ActiveProfileProvider,
        ): EntryScanlatorFilterDataSource =
            AppEntryScanlatorFilterDataSource(entryChapterRepository, handler, profileProvider)

        // Update intervals pace library updates, which desktop doesn't run.
        @Provides
        fun entryUpdateIntervalAvailability(): EntryUpdateIntervalAvailability =
            EntryUpdateIntervalAvailability(isOffered = false)
    }
}
