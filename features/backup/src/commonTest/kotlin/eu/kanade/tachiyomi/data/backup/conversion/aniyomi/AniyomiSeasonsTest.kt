package eu.kanade.tachiyomi.data.backup.conversion.aniyomi

import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AniyomiSeasonsTest {

    @Test
    fun `a parent whose seasons name an id it no longer has is merged by its source without a merge to restore`() {
        // As Aniyomi backs up a library it restored: the seasons still name the parent's id from the older backup
        val seasons = AniyomiSeasons(
            listOf(
                AniyomiSeasonInfo(MOVY, "/tv/37854", "One Piece", id = 168, fetchType = 0),
                AniyomiSeasonInfo(MOVY, "/tv/37854/season/1", "Season 1", favorite = false, parentId = 1037, id = 212),
                AniyomiSeasonInfo(MOVY, "/tv/37854/season/2", "Season 2", favorite = false, parentId = 1037, id = 213),
            ),
        )

        val parent = BackupEntry(source = MOVY, url = "/tv/37854")
        val season = BackupEntry(source = MOVY, url = "/tv/37854/season/1", favorite = false)
        seasons.applyTo(parent, MOVY, parent.url) { it }
        seasons.applyTo(season, MOVY, season.url) { it }

        parent.mergedBySource shouldBe true
        parent.mergeTargetUrl shouldBe null
        season.mergeTargetUrl shouldBe null
        season.favorite shouldBe false
    }

    private companion object {
        const val MOVY = 8881276529694565186L
    }
}
