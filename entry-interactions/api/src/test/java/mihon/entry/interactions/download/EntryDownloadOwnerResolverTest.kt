package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryRepository

class EntryDownloadOwnerResolverTest {
    @Test
    fun `merged children are grouped under their real owners in selection order`() = runBlocking {
        val visible = entry(id = 1L, sourceId = 10L)
        val member = entry(id = 2L, sourceId = 20L)
        val repository = mockk<EntryRepository> {
            coEvery { getEntryById(member.id) } returns member
        }
        val resolver = EntryDownloadOwnerResolver(repository)

        val owners = resolver.resolve(
            visibleEntry = visible,
            children = listOf(chapter(11L, visible.id), chapter(21L, member.id), chapter(22L, member.id)),
        )

        owners.map { it.entry.id }.shouldContainExactly(visible.id, member.id)
        owners[0].children.map(EntryChapter::id).shouldContainExactly(11L)
        owners[1].children.map(EntryChapter::id).shouldContainExactly(21L, 22L)
    }

    @Test
    fun `owners from another profile or content type are rejected`() = runBlocking {
        val visible = entry(id = 1L, profileId = 7L)
        val otherProfile = entry(id = 2L, profileId = 8L)
        val otherType = entry(id = 3L, profileId = 7L, type = EntryType.BOOK)
        val repository = mockk<EntryRepository> {
            coEvery { getEntryById(otherProfile.id) } returns otherProfile
            coEvery { getEntryById(otherType.id) } returns otherType
        }

        val owners = EntryDownloadOwnerResolver(repository).resolve(
            visibleEntry = visible,
            children = listOf(chapter(21L, otherProfile.id), chapter(31L, otherType.id)),
        )

        owners shouldBe emptyList()
    }

    @Test
    fun `queue identity includes profile type owner source and child`() {
        val entry = entry(id = 2L, profileId = 7L, sourceId = 20L)
        val child = chapter(id = 21L, entryId = entry.id)

        EntryDownloadIdentity.from(entry, child) shouldBe EntryDownloadIdentity(
            profileId = 7L,
            entryType = EntryType.MANGA,
            entryId = 2L,
            sourceId = 20L,
            childId = 21L,
        )
    }

    private fun entry(
        id: Long,
        profileId: Long = 1L,
        sourceId: Long = 1L,
        type: EntryType = EntryType.MANGA,
    ): Entry = Entry.create().copy(id = id, profileId = profileId, source = sourceId, type = type)

    private fun chapter(id: Long, entryId: Long): EntryChapter =
        EntryChapter.create().copy(id = id, entryId = entryId)
}
