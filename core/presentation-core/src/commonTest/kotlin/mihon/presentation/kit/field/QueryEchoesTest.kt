package mihon.presentation.kit.field

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class QueryEchoesTest {
    @Test
    fun `a late echo of earlier typing does not overwrite newer characters, but a change from outside still does`() {
        val echoes = QueryEchoes()
        listOf("s", "sh", "sha").forEach(echoes::reported)

        // The caller's query trails the typing; taking "sh" as an outside change would drop the typed "a"
        echoes.isEcho("sh") shouldBe true
        echoes.isEcho("sha") shouldBe true
        echoes.isEcho("") shouldBe false

        listOf("n", "no", "nov").forEach(echoes::reported)
        // A caller that skipped straight to the latest text settles the earlier ones with it
        echoes.isEcho("nov") shouldBe true
        echoes.isEcho("no") shouldBe false
    }
}
