package eu.kanade.tachiyomi.data.backup.restore.restorers

import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.models.BackupViewerSettingOverride
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldContainExactly
import mihon.entry.interactions.reader.settings.MangaReaderSettingsProvider
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class ViewerSettingOverrideRestoreTest {

    @Test
    fun `legacy manga flags become generic overrides`() {
        val backup = BackupEntry(
            url = "/manga",
            title = "Manga",
            artist = null,
            author = null,
            description = null,
            genre = emptyList(),
            status = 0,
            thumbnailUrl = null,
            favorite = true,
            source = 1,
            dateAdded = 0,
            viewerFlags = 27,
            type = EntryType.MANGA,
        )

        backup.toViewerSettingOverrides(Entry.create().copy(id = 10, type = EntryType.MANGA))
            .map { Triple(it.settingId.providerId, it.settingId.key, it.encodedValue) }
            .shouldContainExactly(
                Triple(MangaReaderSettingsProvider.PROVIDER_ID, MangaReaderSettingsProvider.READING_MODE_KEY, "3"),
                Triple(MangaReaderSettingsProvider.PROVIDER_ID, MangaReaderSettingsProvider.ORIENTATION_KEY, "24"),
            )
    }

    @Test
    fun `explicit portable override wins over matching legacy flag`() {
        val backup = BackupEntry(
            url = "/manga",
            title = "Manga",
            artist = null,
            author = null,
            description = null,
            genre = emptyList(),
            status = 0,
            thumbnailUrl = null,
            favorite = true,
            source = 1,
            dateAdded = 0,
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

        backup.toViewerSettingOverrides(Entry.create().copy(id = 10, type = EntryType.MANGA))
            .map { it.settingId.key to it.encodedValue }
            .shouldContainExactly(
                MangaReaderSettingsProvider.READING_MODE_KEY to "5",
                "theme" to "sepia",
                MangaReaderSettingsProvider.ORIENTATION_KEY to "24",
            )
    }
}
