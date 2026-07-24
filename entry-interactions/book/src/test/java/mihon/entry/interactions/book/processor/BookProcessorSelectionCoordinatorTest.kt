package mihon.entry.interactions.book

import android.content.Context
import android.content.Intent
import mihon.book.api.BookContentDescriptor
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class BookProcessorSelectionCoordinatorTest {
    private val descriptor = BookContentDescriptor("application/epub+zip", profile = "reflowable")
    private val otherProfile = descriptor.copy(profile = "fixed-layout")
    private val first = SelectionFakeProcessor("first")
    private val second = SelectionFakeProcessor("second")
    private val preferenceStore = InMemoryPreferenceStore()
    private val preferences = BookProcessorPreferences(preferenceStore)

    @Test
    fun `remembered chooser selection is reused only for the same descriptor`() {
        val coordinator = coordinator(first, second)

        coordinator.choose(descriptor, second.id, remember = true)

        assertEquals(second.id, assertIs<BookProcessorSelection.Selected>(coordinator.resolve(descriptor)).processor.id)
        assertIs<BookProcessorSelection.ChoiceRequired>(coordinator.resolve(otherProfile))
    }

    @Test
    fun `chooser selection is not persisted without explicit remember`() {
        val coordinator = coordinator(first, second)

        coordinator.choose(descriptor, second.id, remember = false)

        assertIs<BookProcessorSelection.ChoiceRequired>(coordinator.resolve(descriptor))
    }

    @Test
    fun `stale remembered processor is cleared and chooser remains available`() {
        preferences.remember(descriptor, "uninstalled")
        val coordinator = coordinator(first, second)

        assertIs<BookProcessorSelection.ChoiceRequired>(coordinator.resolve(descriptor))
        assertEquals(null, preferences.rememberedProcessorId(descriptor))
    }

    @Test
    fun `stale remembered processor is cleared when one compatible processor remains`() {
        preferences.remember(descriptor, "uninstalled")
        val coordinator = coordinator(first)

        assertEquals(first.id, assertIs<BookProcessorSelection.Selected>(coordinator.resolve(descriptor)).processor.id)
        assertEquals(null, preferences.rememberedProcessorId(descriptor))
    }

    @Test
    fun `incompatible chooser selection is rejected`() {
        val coordinator = coordinator(first)

        assertFailsWith<IllegalArgumentException> {
            coordinator.choose(descriptor, "missing", remember = true)
        }
    }

    private fun coordinator(vararg processors: BookProcessor): BookProcessorSelectionCoordinator {
        return BookProcessorSelectionCoordinator(
            registry = BookProcessorRegistry(processors.toList()),
            preferences = preferences,
        )
    }
}

private class SelectionFakeProcessor(
    override val id: String,
) : BookProcessor {
    override val displayName: String = id

    override fun supports(descriptor: BookContentDescriptor): Boolean = true

    override fun createReaderIntent(
        context: Context,
        request: BookReaderRequest,
        sessionToken: String,
    ): Intent = Intent()

    override suspend fun open(content: BookContentSession): BookOpenResult = error("Not used")
}
