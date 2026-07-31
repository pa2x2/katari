package mihon.entry.interactions.book.preparation

import mihon.book.api.BookContentDescriptor
import mihon.book.api.model.BookPublicationModelDescriptor
import mihon.entry.interactions.book.content.BookContentSession
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class BookContentPreparerRegistryTest {
    private val descriptor = BookContentDescriptor("text/html", profile = "prose-chapter")
    private val model = BookPublicationModelDescriptor("book.document")

    @Test
    fun `unique content preparer declares the model used for reader selection`() {
        val preparer = FakePreparer("html", descriptor, model)

        val selected = assertIs<BookContentPreparerSelection.Selected>(
            BookContentPreparerRegistry(
                listOf(
                    preparer,
                ),
            ).resolve(descriptor),
        )

        assertEquals(model, selected.preparer.outputModel)
    }

    @Test
    fun `overlapping preparers are rejected as a deterministic ambiguity`() {
        val registry = BookContentPreparerRegistry(
            listOf(
                FakePreparer("second", descriptor, model),
                FakePreparer("first", descriptor, model),
            ),
        )

        val ambiguous = assertIs<BookContentPreparerSelection.Ambiguous>(registry.resolve(descriptor))

        assertEquals(listOf("first", "second"), ambiguous.preparers.map(BookContentPreparer::id))
    }

    @Test
    fun `unsupported descriptors and duplicate identities fail explicitly`() {
        assertIs<BookContentPreparerSelection.Unsupported>(
            BookContentPreparerRegistry(emptyList())
                .resolve(descriptor),
        )
        assertFailsWith<IllegalArgumentException> {
            BookContentPreparerRegistry(
                listOf(
                    FakePreparer("same", descriptor, model),
                    FakePreparer("same", descriptor, model),
                ),
            )
        }
    }
}

private class FakePreparer(
    override val id: String,
    private val descriptor: BookContentDescriptor,
    override val outputModel: BookPublicationModelDescriptor,
) : BookContentPreparer {
    override fun supports(descriptor: BookContentDescriptor): Boolean = descriptor == this.descriptor

    override suspend fun prepare(content: BookContentSession): BookPreparationResult =
        error("Registry tests never prepare content")
}
