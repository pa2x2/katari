package eu.kanade.tachiyomi.extension.kind

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class LibVersionTest {

    @Test
    fun `extension lib metadata accepts float double and string values`() {
        extensionLibValue(1.6f) shouldBe "1.6"
        extensionLibValue(1.4) shouldBe "1.4"
        extensionLibValue("2.0.1") shouldBe "2.0.1"
    }
}
