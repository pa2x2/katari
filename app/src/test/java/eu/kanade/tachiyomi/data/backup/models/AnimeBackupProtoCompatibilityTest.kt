package eu.kanade.tachiyomi.data.backup.models

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import mihon.feature.profiles.core.ProfileBackup
import mihon.feature.profiles.core.ProfileScopedBackup
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.EntryStatus

class AnimeBackupProtoCompatibilityTest {

    @Test
    fun `legacy top level and profile scoped backup bytes decode with empty anime payload`() {
        val bytes = ProtoBuf.encodeToByteArray(
            serializer = LegacyBackup.serializer(),
            LegacyBackup(
                backupManga = emptyList(),
                backupCategories = emptyList(),
                backupSources = emptyList(),
                backupPreferences = emptyList(),
                backupSourcePreferences = emptyList(),
                backupExtensionStores = emptyList(),
                backupProfiles = emptyList(),
                activeProfileUuid = null,
            ),
        )

        val profileBytes = ProtoBuf.encodeToByteArray(
            serializer = LegacyProfileScopedBackup.serializer(),
            LegacyProfileScopedBackup(
                profile = ProfileBackup(
                    uuid = "uuid",
                    name = "Profile",
                    colorSeed = 1L,
                    position = 1L,
                    requiresAuth = false,
                    isArchived = false,
                ),
                categories = emptyList(),
                manga = emptyList(),
                preferences = emptyList(),
                sourcePreferences = emptyList(),
            ),
        )

        ProtoBuf.decodeFromByteArray(Backup.serializer(), bytes).allEntries() shouldBe emptyList()
        ProtoBuf.decodeFromByteArray(ProfileScopedBackup.serializer(), profileBytes).allEntries() shouldBe emptyList()
    }

    @Test
    fun `legacy manga and anime merge target types default to entry type`() {
        LegacyBackupManga(
            source = 1,
            url = "manga-member",
            mergeTargetSource = 1,
            mergeTargetUrl = "manga-target",
            mergePosition = 0,
        ).toBackupEntry().mergeTargetType shouldBe EntryType.MANGA

        LegacyBackupAnime(
            source = 2,
            url = "anime-member",
            mergeTargetSource = 2,
            mergeTargetUrl = "anime-target",
            mergePosition = 0,
        ).toBackupEntry().mergeTargetType shouldBe EntryType.ANIME
    }

    @Test
    fun `legacy anime statuses map to unified entry statuses`() {
        LegacyBackupAnime(source = 1, url = "cancelled", status = 3)
            .toBackupEntry().status shouldBe EntryStatus.CANCELLED.value
        LegacyBackupAnime(source = 1, url = "on-hiatus", status = 4)
            .toBackupEntry().status shouldBe EntryStatus.ON_HIATUS.value
    }

    @Serializable
    private data class LegacyBackup(
        @ProtoNumber(1) val backupManga: List<LegacyBackupManga> = emptyList(),
        @ProtoNumber(2) val backupCategories: List<BackupCategory> = emptyList(),
        @ProtoNumber(101) val backupSources: List<BackupSource> = emptyList(),
        @ProtoNumber(104) val backupPreferences: List<BackupPreference> = emptyList(),
        @ProtoNumber(105) val backupSourcePreferences: List<BackupSourcePreferences> = emptyList(),
        @ProtoNumber(106) val backupExtensionStores: List<BackupExtensionStore> = emptyList(),
        @ProtoNumber(200) val backupProfiles: List<ProfileScopedBackup> = emptyList(),
        @ProtoNumber(201) val activeProfileUuid: String? = null,
    )

    @Serializable
    private data class LegacyProfileScopedBackup(
        @ProtoNumber(1) val profile: ProfileBackup,
        @ProtoNumber(2) val categories: List<BackupCategory> = emptyList(),
        @ProtoNumber(3) val manga: List<LegacyBackupManga> = emptyList(),
        @ProtoNumber(4) val preferences: List<BackupPreference> = emptyList(),
        @ProtoNumber(5) val sourcePreferences: List<BackupSourcePreferences> = emptyList(),
    )
}
