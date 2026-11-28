package eu.kanade.tachiyomi.data.backup.conversion.aniyomi

import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport
import eu.kanade.tachiyomi.data.backup.conversion.ConvertedSettings
import eu.kanade.tachiyomi.data.backup.conversion.KatariDefaultCategory
import eu.kanade.tachiyomi.data.backup.conversion.mihon.ConvertedCategories
import eu.kanade.tachiyomi.data.backup.conversion.mihon.ConvertedSavedSearches
import eu.kanade.tachiyomi.data.backup.conversion.mihon.MihonManga
import eu.kanade.tachiyomi.data.backup.conversion.mihon.MihonMerges
import eu.kanade.tachiyomi.data.backup.conversion.newer.NewerAppReading
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.identity.SourceIdScheme
import tachiyomi.domain.source.identity.SourceIdentity

class AniyomiConversionReportTest {

    @Test
    fun `an anime source's extension is reported under the identity the stores list it by`() {
        // Aniyomi writes its anime sources without a scheme, which would read as a Tachiyomi source's; this id is a
        // manga source's too
        val mangaSource = BackupSource(name = "MangaDex", sourceId = SHARED_ID)
        val animeSource = BackupSource(name = "Movy", sourceId = SHARED_ID)
        val report = AniyomiConversionReport(MihonMerges.Collector(ProtoBuf, NewerAppReading("Aniyomi")).build())
        report.add(MihonManga(source = SHARED_ID, url = "/title/1"))
        report.add(AniyomiAnime(source = SHARED_ID, url = "/tv/1"))

        val built = report.build(
            backup = AniyomiBackup(
                mangaCategories = emptyList(),
                animeCategories = emptyList(),
                mangaSources = listOf(mangaSource),
                animeSources = listOf(animeSource),
                preferences = emptyList(),
                sourcePreferences = emptyList(),
                extensionSources = emptyList(),
                savedSearches = emptyList(),
                feeds = emptyList(),
                customButtons = emptyList(),
            ),
            categories = ConvertedCategories(
                KatariDefaultCategory(name = "Default", renamed = "Default (Aniyomi)"),
                emptyList(),
                emptyList(),
            ),
            seasons = AniyomiSeasons(emptyList()),
            settings = ConvertedSettings(emptyList(), emptyList(), emptyList()),
            savedSearches = ConvertedSavedSearches(emptyList(), emptyList()),
            extensionApks = 0,
            sourceKeys = AniyomiSourceKeys(listOf(mangaSource), setOf(SHARED_ID), mapOf(SHARED_ID to "Movy")),
        )

        built.rows.single { it.kind == ConversionReport.Kind.INSTALLED_EXTENSIONS }.leftBehind
            .map { it.name as ConversionReport.Name.Source }
            .map { it.text to it.identity } shouldContainExactlyInAnyOrder listOf(
            "MangaDex" to SourceIdentity(SourceIdScheme.TACHIYOMI, SHARED_ID),
            "Movy" to SourceIdentity(SourceIdScheme.ANIYOMI, SHARED_ID),
        )
    }

    private companion object {
        const val SHARED_ID = 8881276529694565186L
    }
}
