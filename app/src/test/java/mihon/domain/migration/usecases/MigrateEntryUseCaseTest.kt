package mihon.domain.migration.usecases

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.entry.EntryType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.domain.migration.models.MigrationFlag
import mihon.entry.interactions.EntryDownloadInteraction
import mihon.entry.interactions.EntryPlaybackPreferencesInteraction
import mihon.entry.interactions.EntryProgressInteraction
import mihon.entry.viewer.settings.ViewerSettingOverrideRepository
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import tachiyomi.core.common.preference.Preference
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.entry.interactor.GetMergedEntry
import tachiyomi.domain.entry.interactor.SyncEntryWithSource
import tachiyomi.domain.entry.interactor.UpdateMergedEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.InsertTrack

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MigrateEntryUseCaseTest {

    @ParameterizedTest(name = "selected remove-download flag deletes {0} downloads when replace={1}")
    @MethodSource("migrationCases")
    fun `selected remove-download flag deletes current entry downloads`(type: EntryType, replace: Boolean) = runTest {
        val fixture = Fixture(setOf(MigrationFlag.REMOVE_DOWNLOAD))
        val current = entry(id = 1L, type = type)
        val target = entry(id = 2L, type = type)

        fixture.useCase(current, target, replace)

        coVerify(exactly = 1) { fixture.downloadInteraction.deleteEntryDownloads(current) }
        coVerify(exactly = 0) { fixture.downloadInteraction.deleteEntryDownloads(target) }
    }

    @ParameterizedTest(name = "unselected remove-download flag preserves {0} downloads when replace={1}")
    @MethodSource("migrationCases")
    fun `unselected remove-download flag preserves current entry downloads`(
        type: EntryType,
        replace: Boolean,
    ) = runTest {
        val fixture = Fixture(emptySet())
        val current = entry(id = 1L, type = type)
        val target = entry(id = 2L, type = type)

        fixture.useCase(current, target, replace)

        coVerify(exactly = 0) { fixture.downloadInteraction.deleteEntryDownloads(any()) }
    }

    @ParameterizedTest(name = "copies viewer settings for {0} when replace={1}")
    @MethodSource("migrationCases")
    fun `viewer setting overrides follow entry migration`(type: EntryType, replace: Boolean) = runTest {
        val fixture = Fixture(emptySet())
        val current = entry(id = 1L, type = type).copy(viewerFlags = 283)
        val target = entry(id = 2L, type = type)

        fixture.useCase(current, target, replace)

        coVerify(exactly = 1) { fixture.viewerSettingOverrideRepository.copy(current.id, target.id) }
        val expectedViewerFlags = if (type == EntryType.MANGA) 256L else 283L
        coVerify(exactly = 1) {
            fixture.entryRepository.update(match { it.id == target.id && it.viewerFlags == expectedViewerFlags })
        }
    }

    private fun migrationCases(): List<Arguments> = EntryType.entries.flatMap { type ->
        listOf(false, true).map { replace ->
            Arguments.of(Named.of(type.name.lowercase(), type), replace)
        }
    }

    private fun entry(id: Long, type: EntryType): Entry = Entry.create().copy(
        id = id,
        type = type,
        source = id,
    )

    private class Fixture(flags: Set<MigrationFlag>) {
        private val sourcePreferences = mockk<SourcePreferences>()
        private val migrationFlags = mockk<Preference<Set<MigrationFlag>>>()
        private val trackerManager = mockk<TrackerManager>()
        private val sourceManager = mockk<SourceManager>()
        val entryRepository = mockk<EntryRepository>(relaxed = true)
        private val entryChapterRepository = mockk<EntryChapterRepository>(relaxed = true)
        private val progressInteraction = mockk<EntryProgressInteraction>(relaxed = true)
        private val playbackPreferencesInteraction = mockk<EntryPlaybackPreferencesInteraction>(relaxed = true)
        val viewerSettingOverrideRepository = mockk<ViewerSettingOverrideRepository>(relaxed = true)
        val downloadInteraction = mockk<EntryDownloadInteraction>(relaxed = true)
        private val categoryRepository = mockk<CategoryRepository>(relaxed = true)
        private val getTracks = mockk<GetTracks>()
        private val insertTrack = mockk<InsertTrack>(relaxed = true)
        private val coverCache = mockk<CoverCache>(relaxed = true)
        private val getMergedEntry = mockk<GetMergedEntry>()
        private val updateMergedEntry = mockk<UpdateMergedEntry>(relaxed = true)
        private val syncEntryWithSource = mockk<SyncEntryWithSource>()

        val useCase = MigrateEntryUseCase(
            sourcePreferences = sourcePreferences,
            trackerManager = trackerManager,
            sourceManager = sourceManager,
            entryRepository = entryRepository,
            entryChapterRepository = entryChapterRepository,
            progressInteraction = progressInteraction,
            playbackPreferencesInteraction = playbackPreferencesInteraction,
            downloadInteraction = downloadInteraction,
            categoryRepository = categoryRepository,
            getTracks = getTracks,
            insertTrack = insertTrack,
            coverCache = coverCache,
            getMergedEntry = getMergedEntry,
            updateMergedEntry = updateMergedEntry,
            syncEntryWithSource = syncEntryWithSource,
            viewerSettingOverrideRepository = viewerSettingOverrideRepository,
        )

        init {
            every { sourcePreferences.migrationFlags } returns migrationFlags
            every { migrationFlags.get() } returns flags
            every { trackerManager.trackers } returns emptyList()
            every { sourceManager.get(any()) } returns null
            coEvery { syncEntryWithSource(any()) } returns SyncEntryWithSource.SyncResult(
                insertedChapters = emptyList(),
                updatedChapters = 0,
                removedChapters = 0,
                hasMetadataChanges = false,
            )
            coEvery { getTracks.await(any()) } returns emptyList()
            coEvery { getMergedEntry.awaitGroupByEntryId(any()) } returns emptyList()
        }
    }
}
