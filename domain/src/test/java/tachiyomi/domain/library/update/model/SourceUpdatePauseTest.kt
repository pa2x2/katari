package tachiyomi.domain.library.update.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SourceUpdatePauseTest {

    // Backups carry the stored values, so a backup made by this version has to restore the same pauses later.
    @Test
    fun `stored pauses read back as written, with 0 standing for until resumed`() {
        SourceUpdatePause.serialize(SourceUpdatePause(2499283573021220255, until = null)) shouldBe
            "2499283573021220255:0"
        SourceUpdatePause.serialize(SourceUpdatePause(-7, until = 1791590400000)) shouldBe "-7:1791590400000"

        SourceUpdatePause.deserialize("2499283573021220255:0") shouldBe SourceUpdatePause(2499283573021220255, null)
        SourceUpdatePause.deserialize("-7:1791590400000") shouldBe SourceUpdatePause(-7, 1791590400000)
        SourceUpdatePause.deserialize("2499283573021220255") shouldBe null
    }
}
