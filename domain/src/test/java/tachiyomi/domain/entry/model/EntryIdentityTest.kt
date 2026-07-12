package tachiyomi.domain.entry.model

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class EntryIdentityTest {

    @Test
    fun `identity distinguishes otherwise identical entries in different profiles`() {
        val first = Entry.create().copy(
            profileId = 1L,
            source = 10L,
            url = "/entry",
            type = EntryType.ANIME,
        )
        val second = first.copy(profileId = 2L)

        first.identity() shouldNotBe second.identity()
    }
}
