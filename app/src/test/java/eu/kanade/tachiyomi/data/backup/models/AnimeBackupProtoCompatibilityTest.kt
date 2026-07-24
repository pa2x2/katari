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
    fun `legacy backup bytes decode with empty anime payload`() {
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

        ProtoBuf.decodeFromByteArray(Backup.serializer(), bytes).allEntries() shouldBe emptyList()
    }

    @Test
    fun `legacy profile scoped backup bytes decode with empty anime payload`() {
        val bytes = ProtoBuf.encodeToByteArray(
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

        ProtoBuf.decodeFromByteArray(ProfileScopedBackup.serializer(), bytes).allEntries() shouldBe emptyList()
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

    @Test
    fun `entry backup bytes preserve merge target type and download preferences`() {
        val bytes = ProtoBuf.encodeToByteArray(
            serializer = BackupEntry.serializer(),
            BackupEntry(
                source = 1,
                url = "anime-member",
                mergeTargetSource = 1,
                mergeTargetUrl = "anime-target",
                mergeTargetType = EntryType.ANIME,
                mergePosition = 1,
                downloadPreferences = BackupDownloadPreferences(
                    dubKey = "dub",
                    streamKey = "stream",
                    subtitleKey = "subtitle",
                    qualityMode = "data_saving",
                    updatedAt = 123,
                ),
                type = EntryType.ANIME,
            ),
        )

        val decoded = ProtoBuf.decodeFromByteArray(BackupEntry.serializer(), bytes)

        decoded.mergeTargetType shouldBe EntryType.ANIME
        decoded.downloadPreferences?.dubKey shouldBe "dub"
        decoded.downloadPreferences?.streamKey shouldBe "stream"
        decoded.downloadPreferences?.subtitleKey shouldBe "subtitle"
        decoded.downloadPreferences?.qualityMode shouldBe "data_saving"
        decoded.downloadPreferences?.updatedAt shouldBe 123
    }

    @Test
    fun `entry backup bytes preserve generic progress extensions`() {
        val extensions = """{"reader.example.precise":"opaque"}""".encodeToByteArray()
        val bytes = ProtoBuf.encodeToByteArray(
            serializer = BackupEntry.serializer(),
            BackupEntry(
                source = 1,
                url = "/entry",
                progressStates = listOf(
                    BackupEntryProgressState(
                        resourceKey = "/chapter",
                        sourceChildKey = "/chapter",
                        locatorKind = "reader.example",
                        progression = 0.5,
                        extensions = extensions,
                        locatorUpdatedAt = 10,
                    ),
                ),
            ),
        )

        val state = ProtoBuf.decodeFromByteArray(BackupEntry.serializer(), bytes).progressStates.single()

        state.resourceKey shouldBe "/chapter"
        state.sourceChildKey shouldBe "/chapter"
        state.locatorKind shouldBe "reader.example"
        state.progression shouldBe 0.5
        state.extensions.contentEquals(extensions) shouldBe true
        state.locatorUpdatedAt shouldBe 10
    }

    @Test
    fun `entry backup bytes preserve unknown Feature state envelopes`() {
        val bytes = ProtoBuf.encodeToByteArray(
            serializer = BackupEntry.serializer(),
            BackupEntry(
                source = 1,
                url = "/entry",
                featureStates = listOf(
                    BackupEntryFeatureState(
                        participantId = "future.feature.backup",
                        schemaVersion = 7,
                        payload = byteArrayOf(1, 2, 3),
                    ),
                ),
            ),
        )

        val state = ProtoBuf.decodeFromByteArray(BackupEntry.serializer(), bytes).featureStates.single()

        state.participantId shouldBe "future.feature.backup"
        state.schemaVersion shouldBe 7
        state.payload.contentEquals(byteArrayOf(1, 2, 3)) shouldBe true
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
