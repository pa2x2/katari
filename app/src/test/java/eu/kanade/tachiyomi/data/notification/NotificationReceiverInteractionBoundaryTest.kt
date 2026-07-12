package eu.kanade.tachiyomi.data.notification

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

class NotificationReceiverInteractionBoundaryTest {

    @Test
    fun `notification entry actions stay routed through entry interactions`() {
        val source = repositoryRoot()
            .resolve("app/src/main/java/eu/kanade/tachiyomi/data/notification/NotificationReceiver.kt")
            .readText()

        assertTrue(source.contains("import mihon.entry.interactions.EntryOpenInteraction"))
        assertTrue(source.contains("import mihon.entry.interactions.EntryConsumptionInteraction"))
        assertTrue(source.contains("import mihon.entry.interactions.EntryDownloadInteraction"))

        assertActionRoutesTo(
            source,
            "ACTION_OPEN_EPISODE",
            """openChild\(context, intent\.legacyEpisodeOpenChildPayload\(\)\)""",
        )
        assertActionRoutesTo(source, "ACTION_OPEN_CHILD", """openChild\(context, intent\.openChildPayload\(\)\)""")
        assertActionRoutesTo(
            source,
            "ACTION_OPEN_CHAPTER",
            """openLegacyChild\(context, intent\.legacyChapterOpenChildPayload\(\)\)""",
        )
        assertTrue(
            source.contains(
                "entryActionHandler.openChild(context, payload.visibleEntryId, payload.ownerEntryId, payload.childId)",
            ),
        )
        assertTrue(
            source.contains(
                "entryActionHandler.openChild(context, visibleEntryId, payload.ownerEntryId, payload.childId)",
            ),
        )

        assertActionRoutesTo(source, "ACTION_MARK_AS_READ", """markConsumed\(intent\.legacyChapterUrlsPayload\(\)\)""")
        assertActionRoutesTo(
            source,
            "ACTION_MARK_AS_WATCHED",
            """markConsumed\(intent\.legacyEpisodeIdsPayload\(\)\)""",
        )
        assertActionRoutesTo(source, "ACTION_MARK_CONSUMED", """markConsumed\(intent\.childIdsPayload\(\)\)""")
        assertTrue(source.contains("entryActionHandler.markConsumed(entryId, childIds)"))
        assertTrue(source.contains("entryActionHandler.markConsumed(entryId, childUrls)"))
        assertTrue(source.contains("entryConsumptionInteraction.setConsumed(entry, chapters, consumed = true)"))

        assertActionRoutesTo(
            source,
            "ACTION_DOWNLOAD_CHAPTER",
            """downloadChildren\(intent\.legacyChapterUrlsPayload\(\)\)""",
        )
        assertActionRoutesTo(source, "ACTION_DOWNLOAD_CHILDREN", """downloadChildren\(intent\.childIdsPayload\(\)\)""")
        assertTrue(source.contains("entryActionHandler.downloadChildren(entryId, childIds)"))
        assertTrue(source.contains("entryActionHandler.downloadChildren(entryId, childUrls)"))
        assertTrue(source.contains("entryDownloadInteraction.download(entry, chapters)"))

        assertFalse(source.contains("import eu.kanade.tachiyomi.source.entry.EntryType"))
        assertFalse(source.contains("EXTRA_ENTRY_TYPE"))
        assertFalse(source.contains("putExtra(EXTRA_ANIME_ID"))
        assertFalse(source.contains("putExtra(EXTRA_MANGA_ID"))
        assertTrue(source.contains("val childIds = loadChildIds(entryId, childUrls)"))
    }

    @Test
    fun `notification receiver does not reference type runtime internals directly`() {
        val source = repositoryRoot()
            .resolve("app/src/main/java/eu/kanade/tachiyomi/data/notification/NotificationReceiver.kt")
            .readText()

        listOf(
            "mihon.entry.interactions.manga.",
            "mihon.entry.interactions.anime.",
            "eu.kanade.tachiyomi.ui.reader.",
            "eu.kanade.tachiyomi.ui.player.",
            "ReaderActivity",
            "PlayerActivity",
            "MangaOpenProcessor",
            "AnimeOpenProcessor",
            "MangaConsumptionProcessor",
            "AnimeConsumptionProcessor",
            "MangaDownloadProcessor",
            "AnimeDownloadProcessor",
        ).forEach { forbiddenReference ->
            assertFalse(
                source.contains(forbiddenReference),
                "NotificationReceiver.kt must not directly reference $forbiddenReference",
            )
        }
    }

    private fun repositoryRoot(): Path {
        return generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .first { Files.exists(it.resolve("settings.gradle.kts")) }
    }

    private fun assertActionRoutesTo(source: String, action: String, callPattern: String) {
        assertTrue(
            Regex("""$action\s*->\s*\{(?:(?!\n\s*ACTION_[A-Z_]+\s*->).)*$callPattern""", RegexOption.DOT_MATCHES_ALL)
                .containsMatchIn(source),
        )
    }
}
