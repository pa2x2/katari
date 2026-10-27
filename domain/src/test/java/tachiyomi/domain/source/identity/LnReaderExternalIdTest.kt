package tachiyomi.domain.source.identity

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class LnReaderExternalIdTest {

    // A plugin's settings files and library entries are keyed by it, so a changed hash orphans them after an update.
    // The expected values are Python's: int.from_bytes(sha256(id).digest()[:8], "big") & (2**63 - 1)
    @Test
    fun `a plugin keeps the external id its settings and entries were stored under`() {
        lnReaderExternalId("novelupdates") shouldBe 5211627521421822759
        lnReaderExternalId("FWN.com") shouldBe 8169655627759657960
    }
}
