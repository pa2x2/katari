package mihon.entry.interactions.book.document.reader

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.AnnotatedString
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class BookDocumentSelectionLanguageContextTest {
    @Test
    fun `selection keeps translated text exact and supplies bounded surrounding prose`() {
        val surroundingText = "a".repeat(1_050) + " Bonjour depuis la vallée."
        val selectedText = annotatedSelection("Bonjour", token = "paragraph")

        val projection = requireNotNull(
            projectBookDocumentSelection(
                ownerIdentity = "owner",
                selectedTexts = listOf(selectedText),
                selectableLeaves = mapOf(
                    "paragraph" to BookDocumentSelectableLeaf(
                        token = "paragraph",
                        chapterId = 1L,
                        fullText = surroundingText,
                        separatorAfter = "\n",
                    ),
                ),
                layouts = emptyMap(),
                readerRootPositionInWindow = Offset.Zero,
            ),
        )

        projection.text shouldBe "Bonjour"
        projection.languageContextText shouldContain "Bonjour depuis la vallée"
        projection.languageContextText.codePointCount(0, projection.languageContextText.length)
            .shouldBeLessThanOrEqual(1_000)
    }

    private fun annotatedSelection(text: String, token: String): AnnotatedString {
        return AnnotatedString.Builder(text).apply {
            addStringAnnotation(
                tag = BOOK_DOCUMENT_SELECTION_TOKEN_TAG,
                annotation = token,
                start = 0,
                end = text.length,
            )
        }.toAnnotatedString()
    }
}
