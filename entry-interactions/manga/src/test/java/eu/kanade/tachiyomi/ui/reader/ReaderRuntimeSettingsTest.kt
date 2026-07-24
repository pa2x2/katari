package eu.kanade.tachiyomi.ui.reader

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ReaderRuntimeSettingsTest {

    @Test
    fun `reading mode changes recreate the viewer`() {
        settings(readingMode = 2)
            .changeFrom(settings(readingMode = 1)) shouldBe ReaderRuntimeSettings.Change.RecreateViewer
    }

    @Test
    fun `orientation-only changes update the existing viewer`() {
        settings(orientation = 2)
            .changeFrom(settings(orientation = 1)) shouldBe ReaderRuntimeSettings.Change.UpdateOrientation
    }

    @Test
    fun `unchanged runtime settings do nothing`() {
        settings().changeFrom(settings()) shouldBe ReaderRuntimeSettings.Change.None
    }

    private fun settings(
        readingMode: Int = 1,
        orientation: Int = 1,
    ) = ReaderRuntimeSettings(
        entryId = 1,
        readingMode = readingMode,
        orientation = orientation,
    )
}
