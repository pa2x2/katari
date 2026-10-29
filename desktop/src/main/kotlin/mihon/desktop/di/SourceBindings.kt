package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.source.service.ProfileHiddenSourceIds
import eu.kanade.presentation.browse.components.SourceRows
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.ExtensionNsfwSourceIds
import eu.kanade.tachiyomi.source.DefaultSourceManager
import kotlinx.coroutines.CoroutineScope
import mihon.core.metro.AppCoroutineScope
import mihon.desktop.source.NoChapterListWalkScheduler
import mihon.desktop.source.NoExtensionSourceRows
import mihon.entry.interactions.download.EntryDownloadMaintenanceFeature
import tachiyomi.domain.entry.service.ChapterListWalkScheduler
import tachiyomi.domain.source.repository.StubSourceRepository
import tachiyomi.domain.source.service.EntryInfoSourceNames
import tachiyomi.domain.source.service.HiddenSourceIds
import tachiyomi.domain.source.service.NsfwSourceIds
import tachiyomi.domain.source.service.SourceInfoNames
import tachiyomi.domain.source.service.SourceManager

@BindingContainer
interface SourceBindings {
    @Binds
    val ProfileHiddenSourceIds.hiddenSourceIds: HiddenSourceIds

    @Binds
    val DefaultSourceManager.sourceManager: SourceManager

    @Binds
    val EntryInfoSourceNames.sourceInfoNames: SourceInfoNames

    @Binds
    val ExtensionNsfwSourceIds.nsfwSourceIds: NsfwSourceIds

    @Binds
    val NoExtensionSourceRows.sourceRows: SourceRows

    @Binds
    val NoChapterListWalkScheduler.chapterListWalkScheduler: ChapterListWalkScheduler

    companion object {
        // Desktop has no local source yet.
        @Provides
        @SingleIn(AppScope::class)
        fun defaultSourceManager(
            extensionManager: ExtensionManager,
            sourceRepository: StubSourceRepository,
            @AppCoroutineScope scope: CoroutineScope,
            downloadMaintenance: Lazy<EntryDownloadMaintenanceFeature>,
        ): DefaultSourceManager = DefaultSourceManager(
            extensionManager = extensionManager,
            sourceRepository = sourceRepository,
            scope = scope,
            downloadMaintenance = downloadMaintenance,
            localSource = null,
        )
    }
}
