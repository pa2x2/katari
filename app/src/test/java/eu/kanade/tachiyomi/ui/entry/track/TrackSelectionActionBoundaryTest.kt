package eu.kanade.tachiyomi.ui.entry.track

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

class TrackSelectionActionBoundaryTest {

    @Test
    fun `tracker selections are captured before non-cancellable dispatch`() {
        val source = repositoryRoot()
            .resolve("app/src/main/java/eu/kanade/tachiyomi/ui/entry/track/TrackInfoDialog.kt")
            .readText()

        Regex(
            """val selection = state\.value\.selection\s+screenModelScope\.launchNonCancellable \{""",
        ).findAll(source).count() shouldBe 3
        assertFalse(source.contains("track.toDbTrack(), state.value.selection"))
    }

    private fun repositoryRoot(): Path {
        return generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .first { Files.exists(it.resolve("settings.gradle.kts")) }
    }
}
