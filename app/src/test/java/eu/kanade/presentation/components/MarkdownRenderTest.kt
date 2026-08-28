package eu.kanade.presentation.components

import io.kotest.matchers.shouldBe
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.CancellationToken
import org.intellij.markdown.parser.MarkdownParser
import org.junit.jupiter.api.Test

class MarkdownRenderTest {

    @Test
    fun `GitHub line endings do not create phantom markdown blocks`() {
        val releaseNotes = """
            ### Added

            - First change

            ### Fixed

            - Second change
        """.trimIndent().replace("\n", "\r\n")
        val normalizedReleaseNotes = normalizeMarkdownLineEndings(releaseNotes)

        MarkdownParser(
            flavour = GFMFlavourDescriptor(),
            cancellationToken = object : CancellationToken {
                override fun checkCancelled() = Unit
            },
        )
            .buildMarkdownTreeFromString(normalizedReleaseNotes as CharSequence)
            .children
            .filter { it.type == MarkdownElementTypes.PARAGRAPH }
            .map { normalizedReleaseNotes.substring(it.startOffset, it.endOffset) } shouldBe emptyList()
    }
}
