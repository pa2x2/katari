package eu.kanade.tachiyomi.data.backup

import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.models.BackupTracking
import eu.kanade.tachiyomi.data.backup.models.LegacyBackupAnime
import eu.kanade.tachiyomi.data.backup.models.LegacyBackupManga
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import mihon.feature.profiles.core.ProfileBackup
import mihon.feature.profiles.core.ProfileScopedBackup
import org.junit.jupiter.api.Test

class BackupFileValidatorTest {

    @Test
    fun `tracking diagnostics inspect current legacy anime and profile entry projections`() {
        val backup = Backup(
            backupManga = listOf(LegacyBackupManga(1, "/legacy", tracking = listOf(track(1)))),
            backupAnime = listOf(LegacyBackupAnime(2, "/legacy-anime")),
            backupEntries = listOf(
                BackupEntry(2, "/anime", type = EntryType.ANIME, tracking = listOf(track(2))),
            ),
            backupProfiles = listOf(
                ProfileScopedBackup(
                    profile = ProfileBackup("profile", "Profile", 1, 0, false, false),
                    manga = listOf(LegacyBackupManga(1, "/profile-legacy", tracking = listOf(track(3)))),
                    entries = listOf(
                        BackupEntry(2, "/profile-anime", type = EntryType.ANIME, tracking = listOf(track(4))),
                    ),
                ),
            ),
        )

        backup.trackingServiceIds() shouldBe setOf(1L, 2L, 3L, 4L)
    }
}

private fun track(id: Int) = BackupTracking(syncId = id, libraryId = 0)
