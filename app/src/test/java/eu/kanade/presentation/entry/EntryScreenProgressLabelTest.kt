package eu.kanade.presentation.entry

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

class EntryScreenProgressLabelTest {

    @Test
    fun `chapter rows resolve supplied child progress labels generically`() {
        val source = repositoryRoot()
            .resolve("app/src/main/java/eu/kanade/presentation/entry/EntryScreen.kt")
            .readText()

        assertTrue(source.contains("childProgressLabels[item.chapter.id]"))
        assertTrue(source.contains("stringResource(it.resource, *it.args.toTypedArray())"))
        assertFalse(source.contains("MR.strings.chapter_progress"))
        assertFalse(source.contains("item.chapter.lastPageRead"))
    }

    private fun repositoryRoot(): Path {
        return generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .first { Files.exists(it.resolve("settings.gradle.kts")) }
    }
}
