package eu.kanade.tachiyomi.data.library

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

class LibraryUpdateJobInteractionBoundaryTest {

    @Test
    fun `auto download filtering is routed through EntryDownloadInteraction`() {
        val source = repositoryRoot()
            .resolve("app/src/main/java/eu/kanade/tachiyomi/data/library/LibraryUpdateJob.kt")
            .readText()

        assertTrue(
            Regex(
                """entryDownloadInteraction\.filterAutoDownloadCandidates\(\s*entry,\s*newChapters,\s*\)""",
                RegexOption.DOT_MATCHES_ALL,
            ).containsMatchIn(source),
        )
        assertTrue(source.contains("entryDownloadInteraction.queue(queuedEntry, chapters, autoStart = false)"))
        assertFalse(source.contains("FilterEntryChaptersForDownload"))
        assertFalse(source.contains("filterChaptersForDownload"))
    }

    private fun repositoryRoot(): Path {
        return generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .first { Files.exists(it.resolve("settings.gradle.kts")) }
    }
}
