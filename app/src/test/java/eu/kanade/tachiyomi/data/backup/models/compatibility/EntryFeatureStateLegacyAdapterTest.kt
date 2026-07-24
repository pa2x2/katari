package eu.kanade.tachiyomi.data.backup.models.compatibility

import eu.kanade.tachiyomi.data.backup.models.BackupChapter
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.models.BackupHistory
import eu.kanade.tachiyomi.data.backup.models.BackupViewerSettingOverride
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import mihon.entry.interactions.ENTRY_PROGRESS_BACKUP_STATE_ID
import mihon.entry.interactions.ENTRY_VIEWER_SETTINGS_BACKUP_STATE_ID
import mihon.entry.interactions.EntryBackupStateCodec
import mihon.entry.interactions.EntryProgressSnapshot
import mihon.entry.interactions.EntryViewerSettingsBackupState
import mihon.entry.interactions.reader.settings.MangaReaderSettingsProvider
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class EntryFeatureStateLegacyAdapterTest {

    @Test
    fun `legacy manga flags and typed overrides become one viewer state`() {
        val backup = BackupEntry(
            source = 1,
            url = "/manga",
            viewerFlags = 27,
            viewerSettingOverrides = listOf(
                BackupViewerSettingOverride(
                    MangaReaderSettingsProvider.PROVIDER_ID,
                    MangaReaderSettingsProvider.READING_MODE_KEY,
                    "5",
                ),
                BackupViewerSettingOverride("unknown.reader", "theme", "sepia"),
                BackupViewerSettingOverride("", "invalid", "ignored"),
                BackupViewerSettingOverride("unknown.reader", "oversized", "x".repeat(16_385)),
            ),
            type = EntryType.MANGA,
        )

        val envelope = backup.featureStatesWithLegacyFallback(Entry.create().copy(id = 10, type = EntryType.MANGA))
            .single { it.participantId == ENTRY_VIEWER_SETTINGS_BACKUP_STATE_ID }
        val state = EntryBackupStateCodec.decode(EntryViewerSettingsBackupState.serializer(), envelope.payload)

        state.overrides.map { it.settingKey to it.encodedValue }.shouldContainExactly(
            MangaReaderSettingsProvider.READING_MODE_KEY to "5",
            "theme" to "sepia",
            MangaReaderSettingsProvider.ORIENTATION_KEY to "24",
        )
    }

    @Test
    fun `legacy chapter and history fields become generic progress state`() {
        val backup = BackupEntry(
            source = 1,
            url = "/manga",
            chapters = listOf(BackupChapter(url = "/chapter", name = "Chapter", lastPageRead = 4)),
            history = listOf(BackupHistory(url = "/chapter", lastRead = 2_000)),
            type = EntryType.MANGA,
        )

        val envelope = backup.featureStatesWithLegacyFallback(Entry.create().copy(type = EntryType.MANGA))
            .single { it.participantId == ENTRY_PROGRESS_BACKUP_STATE_ID }
        val state = EntryBackupStateCodec.decode(EntryProgressSnapshot.serializer(), envelope.payload).states.single()

        state.resourceKey shouldBe "/chapter"
        state.locator.kind shouldBe "page"
        state.locator.position shouldBe 4L
        state.completed shouldBe false
        state.locatorUpdatedAt shouldBe 2_000L
    }

    @Test
    fun `unknown current envelope survives and current state wins over legacy field`() {
        val current = BackupEntry(
            source = 1,
            url = "/entry",
            viewerSettingOverrides = listOf(BackupViewerSettingOverride("legacy", "theme", "dark")),
            featureStates = listOf(
                eu.kanade.tachiyomi.data.backup.models.BackupEntryFeatureState(
                    ENTRY_VIEWER_SETTINGS_BACKUP_STATE_ID,
                    9,
                    byteArrayOf(1),
                ),
                eu.kanade.tachiyomi.data.backup.models.BackupEntryFeatureState(
                    "future.feature.backup",
                    3,
                    byteArrayOf(2),
                ),
            ),
        ).featureStatesWithLegacyFallback(Entry.create())

        current.map { it.participantId to it.schemaVersion } shouldBe listOf(
            ENTRY_VIEWER_SETTINGS_BACKUP_STATE_ID to 9,
            "future.feature.backup" to 3,
        )
    }
}
