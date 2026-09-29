package mihon.entry.interactions.download

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryRepository

class EntryDownloadOwnerResolverTest {
    @Test
    fun `merged children are grouped under their real owners and foreign profile or type owners are rejected`() =
        runBlocking {
            val visible = entry(id = 1L, profileId = 7L, sourceId = 10L)
            val member = entry(id = 2L, profileId = 7L, sourceId = 20L)
            val otherProfile = entry(id = 3L, profileId = 8L)
            val otherType = entry(id = 4L, profileId = 7L, type = EntryType.BOOK)
            val repository = mockk<EntryRepository> {
                listOf(member, otherProfile, otherType).forEach { owner ->
                    coEvery { getEntryById(owner.id) } returns owner
                }
            }

            val owners = EntryDownloadOwnerResolver(repository).resolve(
                visibleEntry = visible,
                children = listOf(
                    chapter(11L, visible.id),
                    chapter(21L, member.id),
                    chapter(31L, otherProfile.id),
                    chapter(22L, member.id),
                    chapter(41L, otherType.id),
                ),
            )

            owners.map { it.entry.id }.shouldContainExactly(visible.id, member.id)
            owners[0].children.map(EntryChapter::id).shouldContainExactly(11L)
            owners[1].children.map(EntryChapter::id).shouldContainExactly(21L, 22L)
        }

    private fun entry(
        id: Long,
        profileId: Long,
        sourceId: Long = 1L,
        type: EntryType = EntryType.MANGA,
    ): Entry = Entry.create().copy(id = id, profileId = profileId, source = sourceId, type = type)

    private fun chapter(id: Long, entryId: Long): EntryChapter =
        EntryChapter.create().copy(id = id, entryId = entryId)
}
