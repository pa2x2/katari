package mihon.entry.interactions.merge.source

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.merge.RecordingEntryMergeHost
import mihon.entry.interactions.merge.host.EntryMergeHostTransition
import mihon.entry.interactions.merge.host.EntryMergeMembershipSnapshot
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class EntryMergeSourceGroupCoordinatorTest {
    /**
     * Every listing used to take a title locked in another merged entry: a box set outside the Library took a Library
     * show's seasons, and the two traded them on every refresh.
     */
    @Test
    fun `a merged entry takes a title from another only when it is in the Library and the other isn't`() = runTest {
        suspend fun boxSetTakesSeason(boxSetInLibrary: Boolean, showInLibrary: Boolean): Boolean {
            val boxSet = entry(BOX_SET, mergedBySource = true, favorite = boxSetInLibrary)
            val film = entry(FILM, favorite = boxSetInLibrary)
            val show = entry(SHOW, mergedBySource = true, favorite = showInLibrary)
            val season = entry(SEASON, favorite = showInLibrary)
            val host = RecordingEntryMergeHost(
                entries = listOf(boxSet, film, show, season),
                memberships = listOf(
                    EntryMergeMembershipSnapshot(PROFILE_ID, BOX_SET, listOf(BOX_SET, FILM)),
                    EntryMergeMembershipSnapshot(PROFILE_ID, SHOW, listOf(SHOW, SEASON)),
                ),
                sourceListings = mapOf(BOX_SET to listOf(FILM, SEASON), SHOW to listOf(SEASON)),
            )

            EntryMergeSourceGroupCoordinator(host) { error("Members already have download titles") }
                .sync(boxSet, listOf(film, season))

            val groups = host.transitions.filterIsInstance<EntryMergeHostTransition.SyncSourceGroup>().single().groups
            val boxSetMembers = groups.single { it.targetEntryId == BOX_SET }.orderedEntryIds
            val showMembers = groups.singleOrNull { it.targetEntryId == SHOW }?.orderedEntryIds
            return if (SEASON in boxSetMembers) {
                showMembers.orEmpty() shouldNotContain SEASON
                true
            } else {
                // Untouched: the show's merge isn't rewritten
                showMembers shouldBe null
                false
            }
        }

        boxSetTakesSeason(boxSetInLibrary = false, showInLibrary = true) shouldBe false
        boxSetTakesSeason(boxSetInLibrary = false, showInLibrary = false) shouldBe false
        boxSetTakesSeason(boxSetInLibrary = true, showInLibrary = true) shouldBe false
        boxSetTakesSeason(boxSetInLibrary = true, showInLibrary = false) shouldBe true
    }

    private fun entry(id: Long, favorite: Boolean, mergedBySource: Boolean = false): Entry {
        return Entry.create().copy(
            id = id,
            profileId = PROFILE_ID,
            favorite = favorite,
            mergedBySource = mergedBySource,
            type = EntryType.ANIME,
            title = "Entry $id",
            downloadTitle = "Entry $id",
        )
    }

    private companion object {
        const val PROFILE_ID = 1L
        const val BOX_SET = 1L
        const val FILM = 2L
        const val SHOW = 3L
        const val SEASON = 4L
    }
}
