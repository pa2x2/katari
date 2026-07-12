package eu.kanade.tachiyomi.ui.library

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR

@RunWith(AndroidJUnit4::class)
class MergeLibraryEntriesDialogTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun existing_merge_selection_is_rendered_as_individual_rows() {
        val dialog = buildMergeDialog(
            listOf(
                libraryManga(
                    id = 1L,
                    title = "Root",
                    memberMangas = listOf(
                        manga(1L, "Root"),
                        manga(2L, "Middle"),
                        manga(3L, "Bottom"),
                    ),
                ),
                libraryManga(id = 4L, title = "New"),
            ),
        ) ?: error("Expected merge dialog")

        composeTestRule.setContent {
            MergeLibraryEntriesDialog(
                dialog = dialog,
                onDismissRequest = {},
                onMove = { _, _ -> },
                onSelectTarget = {},
                onConfirm = {},
            )
        }

        composeTestRule.onNodeWithText("Root").assertIsDisplayed()
        composeTestRule.onNodeWithText("Middle").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bottom").assertIsDisplayed()
        composeTestRule.onNodeWithText("New").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.stringResource(MR.strings.merge_existing_target_locked),
        ).assertIsDisplayed()
        composeTestRule.onAllNodesWithText(
            composeTestRule.activity.stringResource(MR.strings.label_member),
            substring = true,
        ).assertCountEquals(3)
    }
}

private fun libraryManga(
    id: Long,
    title: String,
    memberMangas: List<Manga> = listOf(manga(id = id, title = title)),
): LibraryManga {
    return LibraryManga(
        manga = manga(id = id, title = title),
        categories = listOf(0L),
        totalChapters = 0L,
        readCount = 0L,
        bookmarkCount = 0L,
        latestUpload = 0L,
        chapterFetchedAt = 0L,
        lastRead = 0L,
        memberMangaIds = memberMangas.map(Manga::id),
        memberMangas = memberMangas,
    )
}

private fun manga(id: Long, title: String): Manga {
    return Manga.create().copy(
        id = id,
        source = 0L,
        favorite = true,
        title = title,
    )
}
