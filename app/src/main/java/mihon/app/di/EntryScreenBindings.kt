package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import eu.kanade.presentation.util.AndroidSourceErrorMessages
import eu.kanade.presentation.util.SourceErrorMessages
import eu.kanade.tachiyomi.extension.ExtensionManagerMissingSourceExtensions
import eu.kanade.tachiyomi.ui.entry.cover.AndroidEntryCoverDocuments
import eu.kanade.tachiyomi.ui.entry.cover.AndroidEntryCoverExport
import eu.kanade.tachiyomi.ui.entry.cover.EntryCoverDocuments
import eu.kanade.tachiyomi.ui.entry.cover.EntryCoverExport
import eu.kanade.tachiyomi.ui.entry.missingsource.MissingSourceExtensions
import eu.kanade.tachiyomi.ui.entry.updates.EntryUpdateIntervalAvailability
import eu.kanade.tachiyomi.util.system.isReleaseBuildType

@BindingContainer
interface EntryScreenBindings {
    @Binds
    val AndroidEntryCoverExport.entryCoverExport: EntryCoverExport

    @Binds
    val AndroidEntryCoverDocuments.entryCoverDocuments: EntryCoverDocuments

    @Binds
    val AndroidSourceErrorMessages.sourceErrorMessages: SourceErrorMessages

    @Binds
    val ExtensionManagerMissingSourceExtensions.missingSourceExtensions: MissingSourceExtensions

    companion object {
        // TODO: re-enable when custom intervals are ready for stable
        @Provides
        fun entryUpdateIntervalAvailability(): EntryUpdateIntervalAvailability =
            EntryUpdateIntervalAvailability(isOffered = !isReleaseBuildType)
    }
}
