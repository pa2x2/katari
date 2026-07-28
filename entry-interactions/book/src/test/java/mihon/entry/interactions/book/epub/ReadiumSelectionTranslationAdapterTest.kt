package mihon.entry.interactions.book.epub

import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReadiumSelectionTranslationAdapterTest {
    @Test
    fun `navigator selection bounds translate through the native container into reader root`() {
        val anchor = RectF(10f, 20f, 40f, 60f).toReaderRootAnchor(
            nativeContainerPositionInWindow = Offset(8f, 30f),
            readerRootPositionInWindow = Offset(3f, 12f),
        )

        anchor.left shouldBe 15f
        anchor.top shouldBe 38f
        anchor.right shouldBe 45f
        anchor.bottom shouldBe 78f
    }
}
