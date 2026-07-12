package mihon.feature.profiles.core

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class EntryProfileMoveServiceTest {

    @Test
    fun `moved entry sources become visible without changing unrelated source visibility`() {
        hiddenSourcesAfterMove(
            hiddenSources = setOf("1", "2", "3"),
            movedSourceIds = setOf(1L, 3L),
        ) shouldBe setOf("2")
    }

    @Test
    fun `move identity includes entry type`() {
        val manga = entry(1L, EntryType.MANGA)
        val anime = entry(2L, EntryType.ANIME)

        manga.sameProfileMoveIdentity(manga.copy(id = 3L)) shouldBe true
        manga.sameProfileMoveIdentity(anime) shouldBe false
        manga.sameProfileMoveIdentity(manga.copy(id = 4L, url = "/other")) shouldBe false
        manga.sameProfileMoveIdentity(manga.copy(id = 5L, source = 2L)) shouldBe false
    }

    @Test
    fun `keep source skips an entire merged group`() {
        val first = entry(1L, EntryType.MANGA)
        val second = entry(2L, EntryType.MANGA)
        val destination = entry(10L, EntryType.MANGA)
        val group = EntryProfileMoveGroup(targetEntryId = first.id, entries = listOf(first, second))
        val conflicts = listOf(
            EntryProfileMoveConflict(first, destination, first.id, destinationMergeAffected = false),
        )

        group.shouldSkip(
            conflicts,
            mapOf(first.id to EntryProfileMoveConflictResolution.KEEP_SOURCE),
        ) shouldBe true
        group.shouldSkip(
            conflicts,
            mapOf(first.id to EntryProfileMoveConflictResolution.OVERWRITE_DESTINATION),
        ) shouldBe false
    }

    @Test
    fun `keep source only skips its owning group`() {
        val first = entry(1L, EntryType.MANGA)
        val second = entry(2L, EntryType.MANGA)
        val destination = entry(10L, EntryType.MANGA)
        val conflict = EntryProfileMoveConflict(first, destination, null, destinationMergeAffected = false)

        EntryProfileMoveGroup(null, listOf(second)).shouldSkip(
            listOf(conflict),
            mapOf(first.id to EntryProfileMoveConflictResolution.KEEP_SOURCE),
        ) shouldBe false
    }

    private fun entry(id: Long, type: EntryType): Entry {
        return Entry.create().copy(
            id = id,
            profileId = 1L,
            source = 1L,
            url = "/entry",
            title = "Entry $id",
            favorite = true,
            type = type,
        )
    }
}
