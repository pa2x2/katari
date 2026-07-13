package eu.kanade.tachiyomi.data.backup.create.creators

import eu.kanade.tachiyomi.data.backup.create.BackupOptions
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.protobuf.ProtoBuf
import mihon.entry.interactions.EntryPlaybackPreferencesInteraction
import mihon.entry.interactions.EntryPlaybackPreferencesSnapshot
import mihon.entry.interactions.EntryPlaybackQualityMode
import mihon.entry.interactions.EntryProgressInteraction
import mihon.entry.interactions.EntryProgressSnapshot
import mihon.entry.interactions.EntryProgressStateSnapshot
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.entry.model.DownloadPreferences
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressLocator
import tachiyomi.domain.entry.model.VideoDownloadQualityMode
import tachiyomi.domain.entry.repository.DownloadPreferencesRepository
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EntryBackupCreatorTest {

    @ParameterizedTest(name = "serializes {0} backup with chapters={1}")
    @MethodSource("backupCases")
    fun `playback preferences are independent from chapters only for anime`(
        type: EntryType,
        chaptersEnabled: Boolean,
    ) = runTest {
        val entry = Entry.create().copy(id = 1L, type = type, source = 10L, url = "/entry")
        val chapter = EntryChapter.create().copy(id = 2L, entryId = entry.id, url = "/chapter")
        val fixture = Fixture(entry, chapter)

        val created = fixture.creator.invoke(
            profileId = 1L,
            entries = listOf(entry),
            options = BackupOptions(
                categories = false,
                chapters = chaptersEnabled,
                tracking = false,
                history = false,
            ),
        ).single()
        val bytes = ProtoBuf.encodeToByteArray(BackupEntry.serializer(), created)
        val decoded = ProtoBuf.decodeFromByteArray(BackupEntry.serializer(), bytes)

        decoded.type shouldBe type
        decoded.chapters.map { it.url } shouldBe if (chaptersEnabled) listOf(chapter.url) else emptyList()
        decoded.chapters.map { it.lastPageRead } shouldBe if (chaptersEnabled) listOf(0L) else emptyList()
        decoded.playbackStates shouldBe emptyList()
        decoded.progressStates.map { it.resourceKey } shouldBe if (chaptersEnabled) {
            listOf(chapter.url)
        } else {
            emptyList()
        }

        if (type == EntryType.ANIME) {
            decoded.playbackPreferences?.dubKey shouldBe "playback-dub"
            decoded.playbackPreferences?.streamKey shouldBe "playback-stream"
            decoded.playbackPreferences?.subtitleKey shouldBe "playback-subtitle"
            decoded.playbackPreferences?.playerQualityMode shouldBe "specific_height"
            decoded.playbackPreferences?.playerQualityHeight shouldBe 1080
        } else {
            decoded.playbackPreferences shouldBe null
        }

        if (type == EntryType.ANIME && chaptersEnabled) {
            decoded.downloadPreferences?.dubKey shouldBe "download-dub"
            decoded.downloadPreferences?.qualityMode shouldBe "data_saving"
        } else {
            decoded.downloadPreferences shouldBe null
        }

        coVerify(exactly = if (chaptersEnabled) 1 else 0) {
            fixture.entryChapterRepository.getChaptersByEntryIdAwait(entry.id, applyScanlatorFilter = false)
        }
        coVerify(exactly = if (type == EntryType.ANIME && chaptersEnabled) 1 else 0) {
            fixture.downloadPreferencesRepository.getByEntryId(entry.id)
        }
        coVerify(exactly = if (type == EntryType.ANIME) 1 else 0) {
            fixture.playbackPreferencesInteraction.snapshot(entry)
        }
        coVerify(exactly = if (chaptersEnabled) 1 else 0) {
            fixture.progressInteraction.snapshot(entry)
        }
    }

    private fun backupCases(): List<Arguments> = EntryType.entries.flatMap { type ->
        listOf(false, true).map { chaptersEnabled -> Arguments.of(type, chaptersEnabled) }
    }

    private class Fixture(entry: Entry, chapter: EntryChapter) {
        private val handler = mockk<DatabaseHandler>()
        private val profileProvider = mockk<ActiveProfileProvider>()
        private val entryRepository = mockk<EntryRepository>()
        val entryChapterRepository = mockk<EntryChapterRepository>()
        val downloadPreferencesRepository = mockk<DownloadPreferencesRepository>()
        val progressInteraction = mockk<EntryProgressInteraction>()
        val playbackPreferencesInteraction = mockk<EntryPlaybackPreferencesInteraction>()

        val creator = EntryBackupCreator(
            handler = handler,
            profileProvider = profileProvider,
            entryRepository = entryRepository,
            entryChapterRepository = entryChapterRepository,
            downloadPreferencesRepository = downloadPreferencesRepository,
            progressInteraction = progressInteraction,
            playbackPreferencesInteraction = playbackPreferencesInteraction,
        )

        init {
            coEvery { entryRepository.getAllEntriesByProfile(1L) } returns listOf(entry)
            coEvery {
                entryChapterRepository.getChaptersByEntryIdAwait(entry.id, applyScanlatorFilter = false)
            } returns listOf(chapter)
            coEvery { downloadPreferencesRepository.getByEntryId(entry.id) } returns DownloadPreferences(
                entryId = entry.id,
                dubKey = "download-dub",
                streamKey = "download-stream",
                subtitleKey = "download-subtitle",
                qualityMode = VideoDownloadQualityMode.DATA_SAVING,
                updatedAt = 20L,
            )
            coEvery { playbackPreferencesInteraction.snapshot(entry) } returns if (entry.type == EntryType.ANIME) {
                EntryPlaybackPreferencesSnapshot(
                    dubKey = "playback-dub",
                    streamKey = "playback-stream",
                    subtitleKey = "playback-subtitle",
                    playerQualityMode = EntryPlaybackQualityMode.SPECIFIC_HEIGHT,
                    playerQualityHeight = 1080,
                    updatedAt = 10L,
                )
            } else {
                null
            }
            coEvery { progressInteraction.snapshot(entry) } returns EntryProgressSnapshot(
                states = listOf(
                    EntryProgressStateSnapshot(
                        resourceKey = chapter.url,
                        sourceChildKey = chapter.url,
                        locator = EntryProgressLocator(kind = "page", position = 4),
                        locatorUpdatedAt = 10,
                    ),
                ),
            )
            coEvery { handler.awaitList<Any>(false, any()) } returns emptyList()
        }
    }
}
