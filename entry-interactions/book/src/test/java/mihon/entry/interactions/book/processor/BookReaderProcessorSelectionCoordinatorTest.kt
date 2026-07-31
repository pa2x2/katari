package mihon.entry.interactions.book.processor

import android.content.Context
import android.content.Intent
import mihon.book.api.model.BookPublicationModelDescriptor
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class BookReaderProcessorSelectionCoordinatorTest {
    private val descriptor = BookPublicationModelDescriptor("book.alternate")
    private val otherProfile = BookPublicationModelDescriptor("book.fixed-page")
    private val first = SelectionFakeProcessor("first")
    private val second = SelectionFakeProcessor("second")
    private val preferenceStore = InMemoryPreferenceStore()
    private val preferences = BookReaderProcessorPreferences(preferenceStore)

    @Test
    fun `remembered chooser selection is reused only for the same descriptor`() {
        val coordinator = coordinator(first, second)

        coordinator.choose(descriptor, second.id, remember = true)

        assertEquals(
            second.id,
            assertIs<BookReaderProcessorSelection.Selected>(coordinator.resolve(descriptor)).processor.id,
        )
        assertIs<BookReaderProcessorSelection.ChoiceRequired>(coordinator.resolve(otherProfile))
    }

    @Test
    fun `chooser selection is not persisted without explicit remember`() {
        val coordinator = coordinator(first, second)

        coordinator.choose(descriptor, second.id, remember = false)

        assertIs<BookReaderProcessorSelection.ChoiceRequired>(coordinator.resolve(descriptor))
    }

    @Test
    fun `stale remembered processor is cleared and chooser remains available`() {
        preferences.remember(descriptor, "uninstalled")
        val coordinator = coordinator(first, second)

        assertIs<BookReaderProcessorSelection.ChoiceRequired>(coordinator.resolve(descriptor))
        assertEquals(null, preferences.rememberedProcessorId(descriptor))
    }

    @Test
    fun `stale remembered processor is cleared when one compatible processor remains`() {
        preferences.remember(descriptor, "uninstalled")
        val coordinator = coordinator(first)

        assertEquals(
            first.id,
            assertIs<BookReaderProcessorSelection.Selected>(coordinator.resolve(descriptor)).processor.id,
        )
        assertEquals(null, preferences.rememberedProcessorId(descriptor))
    }

    @Test
    fun `incompatible chooser selection is rejected`() {
        val coordinator = coordinator(first)

        assertFailsWith<IllegalArgumentException> {
            coordinator.choose(descriptor, "missing", remember = true)
        }
    }

    private fun coordinator(vararg processors: BookReaderProcessor): BookReaderProcessorSelectionCoordinator {
        return BookReaderProcessorSelectionCoordinator(
            registry = BookReaderProcessorRegistry(processors.toList()),
            preferences = preferences,
        )
    }
}

private class SelectionFakeProcessor(
    override val id: String,
) : BookReaderProcessor {
    override val displayName: String = id

    override fun supports(model: BookPublicationModelDescriptor): Boolean = true

    override fun createReaderIntent(
        context: Context,
        request: BookReaderRequest,
        sessionToken: String,
    ): Intent = Intent()
}
