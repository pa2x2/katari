package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.domain.source.service.ProfileHiddenSourceIds
import eu.kanade.presentation.browse.components.SourceRows
import mihon.desktop.source.NoChapterListWalkScheduler
import mihon.desktop.source.NoExtensionSourceRows
import mihon.desktop.source.NoNsfwSourceIds
import mihon.desktop.source.NoSourceDescriptions
import mihon.desktop.source.NoSourceManager
import tachiyomi.domain.entry.service.ChapterListWalkScheduler
import tachiyomi.domain.source.service.EntryInfoSourceNames
import tachiyomi.domain.source.service.EntrySourceDescriptionResolutionPort
import tachiyomi.domain.source.service.HiddenSourceIds
import tachiyomi.domain.source.service.NsfwSourceIds
import tachiyomi.domain.source.service.SourceInfoNames
import tachiyomi.domain.source.service.SourceManager

@BindingContainer
interface SourceBindings {
    @Binds
    val ProfileHiddenSourceIds.hiddenSourceIds: HiddenSourceIds

    @Binds
    val NoSourceManager.sourceManager: SourceManager

    @Binds
    val EntryInfoSourceNames.sourceInfoNames: SourceInfoNames

    @Binds
    val NoNsfwSourceIds.nsfwSourceIds: NsfwSourceIds

    @Binds
    val NoSourceDescriptions.sourceDescriptions: EntrySourceDescriptionResolutionPort

    @Binds
    val NoExtensionSourceRows.sourceRows: SourceRows

    @Binds
    val NoChapterListWalkScheduler.chapterListWalkScheduler: ChapterListWalkScheduler
}
