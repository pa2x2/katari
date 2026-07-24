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
import mihon.entry.interactions.ENTRY_MERGE_BACKUP_STATE_ID
import mihon.entry.interactions.EntryBackupFeature
import mihon.entry.interactions.EntryBackupSelection
import mihon.entry.interactions.EntryBackupStateCodec
import mihon.entry.interactions.EntryFeatureStateEnvelope
import mihon.entry.interactions.EntryMergeBackupIdentity
import mihon.entry.interactions.EntryMergeBackupMember
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

    private fun chapterCases(): List<Arguments> = listOf(Arguments.of(false), Arguments.of(true))

    private class Fixture(entry: Entry, chapter: EntryChapter) {
        private val handler = mockk<DatabaseHandler>()
        private val profileProvider = mockk<ActiveProfileProvider>()
        val entryBackupFeature = mockk<EntryBackupFeature>()
        val entryChapterRepository = mockk<EntryChapterRepository>()

        val creator = EntryBackupCreator(
            handler = handler,
            profileProvider = profileProvider,
            entryBackupFeature = entryBackupFeature,
            entryChapterRepository = entryChapterRepository,
        )

        init {
            coEvery { entryBackupFeature.snapshot(any(), any(), any()) } returns emptyList()
            coEvery {
                entryChapterRepository.getChaptersByEntryIdAwait(entry.id, applyScanlatorFilter = false)
            } returns listOf(chapter)
            coEvery { handler.awaitList<Any>(false, any()) } returns emptyList()
        }
    }
}
