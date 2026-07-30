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
    fun `navigator selection bounds translate from navigator view into reader root`() {
        val anchor = RectF(10f, 20f, 40f, 60f).toReaderRootAnchor(
            navigatorViewPositionInWindow = Offset(8f, 30f),
            readerRootPositionInWindow = Offset(3f, 12f),
        )

        anchor.left shouldBe 15f
        anchor.top shouldBe 38f
        anchor.right shouldBe 45f
        anchor.bottom shouldBe 78f
    }

    @Test
    fun `navigator selection bounds restore Readium's missing bottom inset`() {
        val anchor = RectF(10f, 38f, 40f, 60f).toReaderRootAnchor(
            navigatorViewPositionInWindow = Offset(8f, 30f),
            readerRootPositionInWindow = Offset(3f, 12f),
            readiumContentTopInset = 18f,
        )

        anchor.left shouldBe 15f
        anchor.top shouldBe 56f
        anchor.right shouldBe 45f
        anchor.bottom shouldBe 96f
    }
}
