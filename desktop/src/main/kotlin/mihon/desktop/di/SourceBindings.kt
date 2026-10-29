package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.presentation.browse.components.SourceRows
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.source.DefaultSourceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import mihon.core.metro.AppCoroutineScope
import mihon.desktop.source.DesktopSourceRows
import mihon.desktop.source.InProcessChapterListWalkScheduler
import mihon.desktop.storage.StorageLocationFolders
import mihon.desktop.work.ResumableWork
import mihon.entry.interactions.download.EntryDownloadMaintenanceFeature
import nl.adaptivity.xmlutil.serialization.XML
import tachiyomi.domain.entry.service.ChapterListWalkScheduler
import tachiyomi.domain.source.repository.StubSourceRepository
import tachiyomi.i18n.localized.stringResource
import tachiyomi.source.local.LocalSource
import tachiyomi.source.local.image.LocalCoverManager
import tachiyomi.source.local.image.NoMediaScanner
import tachiyomi.source.local.io.LocalSourceFileSystem
import tachiyomi.source.local.io.PathArchiveOpener

@BindingContainer
interface SourceBindings {
    @Binds
    val DesktopSourceRows.sourceRows: SourceRows

    @Binds
    val InProcessChapterListWalkScheduler.chapterListWalkScheduler: ChapterListWalkScheduler

    @Binds
    @IntoSet
    val InProcessChapterListWalkScheduler.resumableChapterListWalks: ResumableWork

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun localSourceFileSystem(folders: StorageLocationFolders): LocalSourceFileSystem =
            LocalSourceFileSystem(folders::localSource)

        @Provides
        @SingleIn(AppScope::class)
        fun localCoverManager(fileSystem: LocalSourceFileSystem): LocalCoverManager =
            LocalCoverManager(fileSystem, NoMediaScanner)

        @Provides
        @SingleIn(AppScope::class)
        fun defaultSourceManager(
            extensionManager: ExtensionManager,
            sourceRepository: StubSourceRepository,
            @AppCoroutineScope scope: CoroutineScope,
            downloadMaintenance: Lazy<EntryDownloadMaintenanceFeature>,
            localSourceFileSystem: Lazy<LocalSourceFileSystem>,
            localCoverManager: Lazy<LocalCoverManager>,
            json: Json,
            xml: XML,
        ): DefaultSourceManager = DefaultSourceManager(
            extensionManager = extensionManager,
            sourceRepository = sourceRepository,
            scope = scope,
            downloadMaintenance = downloadMaintenance,
            localSource = {
                LocalSource(
                    fileSystem = localSourceFileSystem.value,
                    coverManager = localCoverManager.value,
                    archives = PathArchiveOpener,
                    json = json,
                    xml = xml,
                    localize = ::stringResource,
                )
            },
        )
    }
}
