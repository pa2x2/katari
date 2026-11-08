package app.katari.desktop.di

import app.katari.core.archive.ArchiveOpener
import app.katari.core.metro.AppCoroutineScope
import app.katari.desktop.source.InProcessChapterListWalkScheduler
import app.katari.desktop.work.ResumableWork
import app.katari.entry.interactions.download.EntryDownloadMaintenanceFeature
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.source.DefaultSourceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import nl.adaptivity.xmlutil.serialization.XML
import tachiyomi.domain.entry.service.ChapterListWalkScheduler
import tachiyomi.domain.source.repository.StubSourceRepository
import tachiyomi.i18n.localized.stringResource
import tachiyomi.source.local.LocalSource
import tachiyomi.source.local.image.LocalCoverManager
import tachiyomi.source.local.io.LocalSourceFileSystem

@BindingContainer
interface SourceBindings {
    @Binds
    val InProcessChapterListWalkScheduler.chapterListWalkScheduler: ChapterListWalkScheduler

    @Binds
    @IntoSet
    val InProcessChapterListWalkScheduler.resumableChapterListWalks: ResumableWork

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun defaultSourceManager(
            extensionManager: ExtensionManager,
            sourceRepository: StubSourceRepository,
            @AppCoroutineScope scope: CoroutineScope,
            downloadMaintenance: Lazy<EntryDownloadMaintenanceFeature>,
            localSourceFileSystem: Lazy<LocalSourceFileSystem>,
            localCoverManager: Lazy<LocalCoverManager>,
            archives: ArchiveOpener,
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
                    archives = archives,
                    json = json,
                    xml = xml,
                    localize = ::stringResource,
                )
            },
        )
    }
}
