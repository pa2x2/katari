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
import mihon.entry.interactions.merge.ENTRY_MERGE_BACKUP_STATE_ID
import mihon.entry.interactions.merge.EntryMergeBackupIdentity
import mihon.entry.interactions.merge.EntryMergeBackupMember
import mihon.entry.interactions.persistence.backup.EntryBackupFeature
import mihon.entry.interactions.persistence.backup.EntryBackupSelection
import mihon.entry.interactions.persistence.backup.EntryBackupStateCodec
import mihon.entry.interactions.persistence.backup.EntryFeatureStateEnvelope
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.history.model.activity.HistoryActivitySegmentSnapshot
import tachiyomi.domain.history.model.activity.HistoryActivitySessionSnapshot
import tachiyomi.domain.history.model.activity.HistoryActivitySnapshot
import tachiyomi.domain.history.model.activity.HistoryCompletionCause
import tachiyomi.domain.history.model.activity.HistoryCompletionSnapshot
import tachiyomi.domain.history.repository.HistoryActivityBackupRepository

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EntryBackupCreatorTest {

    @Test
    fun `discovered states are serialized and known state is projected for legacy readers`() = runTest {
        val entry = Entry.create().copy(id = 1L, type = EntryType.BOOK, source = 10L, url = "/entry")
        val fixture = Fixture(entry, EntryChapter.create().copy(id = 2L, entryId = entry.id, url = "/chapter"))
        val merge = EntryMergeBackupMember(
            target = EntryMergeBackupIdentity(20L, "/target", EntryType.BOOK),
            position = 3,
        )
        coEvery { fixture.entryBackupFeature.snapshot(1L, entry, any()) } returns listOf(
            EntryFeatureStateEnvelope(
                participantId = ENTRY_MERGE_BACKUP_STATE_ID,
                schemaVersion = 1,
                payload = EntryBackupStateCodec.encode(EntryMergeBackupMember.serializer(), merge),
            ),
            EntryFeatureStateEnvelope("future.feature.backup", 7, byteArrayOf(1, 2, 3)),
        )

        val created = fixture.creator.invoke(
            profileId = 1L,
            entries = listOf(entry),
            options = BackupOptions(categories = false, chapters = false, tracking = false, history = false),
        ).single()
        val decoded = ProtoBuf.decodeFromByteArray(
            BackupEntry.serializer(),
            ProtoBuf.encodeToByteArray(BackupEntry.serializer(), created),
        )

        decoded.featureStates.map { it.participantId to it.schemaVersion } shouldBe listOf(
            ENTRY_MERGE_BACKUP_STATE_ID to 1,
            "future.feature.backup" to 7,
        )
        decoded.mergeTargetSource shouldBe 20L
        decoded.mergeTargetUrl shouldBe "/target"
        decoded.mergeTargetType shouldBe EntryType.BOOK
        decoded.mergePosition shouldBe 3
    }

    @ParameterizedTest(name = "passes content selection and serializes core chapters={0}")
    @MethodSource("chapterCases")
    fun `creator delegates feature state without enumerating participants`(chaptersEnabled: Boolean) = runTest {
        val entry = Entry.create().copy(id = 1L, type = EntryType.ANIME, source = 10L, url = "/entry")
        val chapter = EntryChapter.create().copy(id = 2L, entryId = entry.id, url = "/chapter")
        val fixture = Fixture(entry, chapter)

        val created = fixture.creator.invoke(
            profileId = 1L,
            entries = listOf(entry),
            options = BackupOptions(
                categories = false,
                chapters = chaptersEnabled,
                tracking = true,
                history = false,
            ),
        ).single()

        created.chapters.map { it.url } shouldBe if (chaptersEnabled) listOf(chapter.url) else emptyList()
        coVerify(exactly = 1) {
            fixture.entryBackupFeature.snapshot(
                1L,
                entry,
                EntryBackupSelection(includeContentState = chaptersEnabled, includeTrackingState = true),
            )
        }
        coVerify(exactly = if (chaptersEnabled) 1 else 0) {
            fixture.entryChapterRepository.getChaptersByEntryIdAwait(entry.id, applyScanlatorFilter = false)
        }
    }

    @Test
    fun `history selection serializes portable detailed activity and profile epoch`() = runTest {
        val entry = Entry.create().copy(id = 1L, type = EntryType.MANGA, source = 10L, url = "/entry")
        val chapter = EntryChapter.create().copy(id = 2L, entryId = entry.id, url = "/chapter")
        val fixture = Fixture(entry, chapter)
        coEvery { fixture.activityBackupRepository.getActivityByEntryId(entry.id) } returns HistoryActivitySnapshot(
            sessions = listOf(
                HistoryActivitySessionSnapshot(
                    sessionId = "session",
                    startedAtEpochMillis = 1_000L,
                    endedAtEpochMillis = 2_000L,
                    durationMillis = 1_000L,
                    lastSequence = 1L,
                    segments = listOf(
                        HistoryActivitySegmentSnapshot(
                            chapterId = chapter.id,
                            localDate = "2026-08-23",
                            timeZoneId = "UTC",
                            startedAtEpochMillis = 1_000L,
                            endedAtEpochMillis = 2_000L,
                            durationMillis = 1_000L,
                        ),
                    ),
                ),
            ),
            completions = listOf(
                HistoryCompletionSnapshot(
                    eventId = "completion",
                    chapterId = chapter.id,
                    sessionId = "session",
                    occurredAtEpochMillis = 2_000L,
                    localDate = "2026-08-23",
                    timeZoneId = "UTC",
                    cause = HistoryCompletionCause.CONSUMPTION,
                ),
            ),
        )
        coEvery { fixture.activityBackupRepository.getStatisticsEpoch(1L) } returns 500L

        val created = fixture.creator.invoke(
            profileId = 1L,
            entries = listOf(entry),
            options = BackupOptions(categories = false, chapters = false, tracking = false, history = true),
        ).single()

        created.activitySessions.single().segments.single().chapterUrl shouldBe chapter.url
        created.activityCompletions.single().chapterUrl shouldBe chapter.url
        created.activityCompletions.single().cause shouldBe "consumption"
        created.statisticsEpoch shouldBe 500L
    }

    private fun chapterCases(): List<Arguments> = listOf(Arguments.of(false), Arguments.of(true))

    private class Fixture(entry: Entry, chapter: EntryChapter) {
        private val handler = mockk<DatabaseHandler>()
        private val profileProvider = mockk<ActiveProfileProvider>()
        val entryBackupFeature = mockk<EntryBackupFeature>()
        val entryChapterRepository = mockk<EntryChapterRepository>()
        val activityBackupRepository = mockk<HistoryActivityBackupRepository>()

        val creator = EntryBackupCreator(
            handler = handler,
            profileProvider = profileProvider,
            entryBackupFeature = entryBackupFeature,
            entryChapterRepository = entryChapterRepository,
            activityBackupRepository = activityBackupRepository,
        )

        init {
            coEvery { entryBackupFeature.snapshot(any(), any(), any()) } returns emptyList()
            coEvery {
                entryChapterRepository.getChaptersByEntryIdAwait(entry.id, applyScanlatorFilter = false)
            } returns listOf(chapter)
            coEvery { entryChapterRepository.getChapterById(chapter.id) } returns chapter
            coEvery { handler.awaitList<Any>(false, any()) } returns emptyList()
            coEvery { activityBackupRepository.getActivityByEntryId(entry.id) } returns
                HistoryActivitySnapshot(emptyList(), emptyList())
            coEvery { activityBackupRepository.getStatisticsEpoch(any()) } returns null
        }
    }
}
