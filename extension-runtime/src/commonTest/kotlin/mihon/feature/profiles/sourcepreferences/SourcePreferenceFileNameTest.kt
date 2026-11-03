package mihon.feature.profiles.sourcepreferences

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SourcePreferenceFileNameTest {

    // Catching Katari's own per-profile file would nest it into another profile's file name
    @Test
    fun `only the name an extension opens itself is redirected`() {
        extensionPreferenceSourceId("source_6202325652827735606") shouldBe 6202325652827735606
        extensionPreferenceSourceId("source_-5") shouldBe -5
        extensionPreferenceSourceId(sourcePreferenceFileName(1, 6202325652827735606)) shouldBe null
        extensionPreferenceSourceId("source_6202325652827735606_cache") shouldBe null
    }
}
