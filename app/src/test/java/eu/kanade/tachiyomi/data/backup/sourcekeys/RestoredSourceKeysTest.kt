package eu.kanade.tachiyomi.data.backup.sourcekeys

import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringSetPreferenceValue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.identity.RegisteredSource
import tachiyomi.domain.source.identity.SourceIdScheme
import tachiyomi.domain.source.identity.SourceIdentity
import tachiyomi.domain.source.identity.SourceIdentityRepository
import tachiyomi.domain.source.identity.SourceProvider

class RestoredSourceKeysTest {

    @Test
    fun `a source with another key on this device brings its library, pins and settings to that key`() = runTest {
        val anime = SourceIdentity(SourceIdScheme("aniyomi"), 77)
        val backup = roundTrip(
            Backup(
                backupSources = listOf(
                    BackupSource("Anime", sourceId = -2, idScheme = "aniyomi", externalId = 77),
                    BackupSource("Manga", sourceId = 77, idScheme = "tachiyomi", externalId = 77),
                ),
                backupEntries = listOf(
                    BackupEntry(source = -2, url = "/anime"),
                    BackupEntry(source = 77, url = "/manga"),
                ),
                backupPreferences = listOf(
                    BackupPreference("pinned_sources", StringSetPreferenceValue(setOf("-2", "77"))),
                    BackupPreference("pref_display_mode_catalogue_-2", StringPreferenceValue("LIST")),
                    BackupPreference("library_update_paused_sources", StringSetPreferenceValue(setOf("-2:0"))),
                    BackupPreference(
                        "saved_source_feeds",
                        StringPreferenceValue("""[{"id":"f","sourceId":-2,"presetId":"popular"}]"""),
                    ),
                ),
                backupSourcePreferences = listOf(BackupSourcePreferences("source_1_-2", emptyList(), sourceId = -2)),
            ),
        )
        val device = DeviceRegistry(mapOf(anime to -5L, SourceIdentity(SourceIdScheme.TACHIYOMI, 77) to 77L))

        val restored = RestoredSourceKeys.of(backup, device).applyTo(backup)

        restored.allEntries().map { it.source } shouldContainExactly listOf(-5L, 77L)
        restored.backupPreferences.associate { it.key to it.value } shouldBe mapOf(
            "pinned_sources" to StringSetPreferenceValue(setOf("-5", "77")),
            "pref_display_mode_catalogue_-5" to StringPreferenceValue("LIST"),
            "library_update_paused_sources" to StringSetPreferenceValue(setOf("-5:0")),
            "saved_source_feeds" to StringPreferenceValue("""[{"id":"f","sourceId":-5,"presetId":"popular"}]"""),
        )
        restored.backupSourcePreferences.single().sourceId shouldBe -5L
    }

    @Test
    fun `a backup without identities restores every key as the tachiyomi or local source it was`() = runTest {
        val bytes = ProtoBuf.encodeToByteArray(
            OldBackup.serializer(),
            OldBackup(
                backupEntries = listOf(
                    OldBackupEntry(source = 0, url = "/local"),
                    OldBackupEntry(source = 123, url = "/a"),
                ),
                backupSources = listOf(OldBackupSource("Local", 0), OldBackupSource("Some source", 123)),
            ),
        )
        val backup = ProtoBuf.decodeFromByteArray(Backup.serializer(), bytes)
        val device = DeviceRegistry(
            mapOf(SourceIdentity.LOCAL to 0L, SourceIdentity(SourceIdScheme.TACHIYOMI, 123) to 123L),
        )

        val restored = RestoredSourceKeys.of(backup, device).applyTo(backup)

        restored.allEntries().map { it.source } shouldContainExactly listOf(0L, 123L)
        device.registered shouldContainExactlyInAnyOrder device.keys.keys
    }

    private fun roundTrip(backup: Backup): Backup {
        return ProtoBuf.decodeFromByteArray(
            Backup.serializer(),
            ProtoBuf.encodeToByteArray(Backup.serializer(), backup),
        )
    }

    /** The registry of the device restored to, with the keys it already gave out. */
    private class DeviceRegistry(val keys: Map<SourceIdentity, Long>) : SourceIdentityRepository {
        val registered = mutableSetOf<SourceIdentity>()

        override suspend fun register(identity: SourceIdentity): Long {
            registered += identity
            return keys.getValue(identity)
        }

        override suspend fun find(localKey: Long): RegisteredSource? = error("Not used by restore")

        override suspend fun findAll(): List<RegisteredSource> = error("Not used by restore")

        override suspend fun recordProvider(localKey: Long, provider: SourceProvider) = error("Not used by restore")
    }

    // Backups as Katari wrote them before identities were recorded, and as Mihon writes them
    @Serializable
    private class OldBackup(
        @ProtoNumber(101) val backupSources: List<OldBackupSource>,
        @ProtoNumber(203) val backupEntries: List<OldBackupEntry>,
    )

    @Serializable
    private class OldBackupSource(
        @ProtoNumber(1) val name: String,
        @ProtoNumber(2) val sourceId: Long,
    )

    @Serializable
    private class OldBackupEntry(
        @ProtoNumber(1) val source: Long,
        @ProtoNumber(2) val url: String,
    )
}
