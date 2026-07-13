package mihon.entry.interactions.book

import android.content.Context
import android.content.Intent
import eu.kanade.tachiyomi.source.entry.EntryType
import io.mockk.mockk
import io.mockk.verify
import mihon.entry.interactions.EntryOpenOptions
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class BookOpenProcessorTest {
    private val context = mockk<Context>(relaxed = true)
    private val chapter = EntryChapter.create().copy(id = 2L)

    @Test
    fun `open routes BOOK content to dedicated unavailable host`() {
        val target = Intent("test.book.unavailable")
        val processor = BookOpenProcessor { _, _, _ -> target }

        processor.open(
            context,
            Entry.create().copy(id = 1L, type = EntryType.BOOK),
            chapter,
            EntryOpenOptions(newTask = true, clearTop = true),
        )

        verify { context.startActivity(target) }
        assert(target.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assert(target.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
    }

    @Test
    fun `open rejects a non-BOOK entry`() {
        val processor = BookOpenProcessor { _, _, _ -> Intent() }

        assertFailsWith<IllegalArgumentException> {
            processor.open(context, Entry.create().copy(type = EntryType.MANGA), chapter, EntryOpenOptions())
        }
    }
}
