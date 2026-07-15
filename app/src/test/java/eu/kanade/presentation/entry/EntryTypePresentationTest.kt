package eu.kanade.presentation.entry

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Info
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import tachiyomi.i18n.MR

class EntryTypePresentationTest {

    @Test
    fun `missing child gaps are reported only for manga`() {
        val childNumbers = listOf(1.0, 3.0)

        missingChildCount(EntryType.MANGA, childNumbers) shouldBe 1
        missingChildCount(EntryType.ANIME, childNumbers) shouldBe 0
        missingChildCount(EntryType.BOOK, childNumbers) shouldBe 0
    }

    @Test
    fun `manga presentation uses existing manga labels and icons`() {
        val presentation = EntryType.MANGA.entryTypePresentation()

        presentation.displayNameLabel shouldBe MR.strings.entry_type_manga
        presentation.badgeIcon shouldBe Icons.AutoMirrored.Outlined.MenuBook
        presentation.smallIcon shouldBe R.drawable.ic_menu_book_24dp
        presentation.coverOverlayIcon shouldBe null
        presentation.markAsConsumedLabel shouldBe MR.strings.action_mark_as_read
        presentation.markAsUnconsumedLabel shouldBe MR.strings.action_mark_as_unread
        presentation.markPreviousAsConsumedLabel shouldBe MR.strings.action_mark_previous_as_read
        presentation.unconsumedIndicatorLabel shouldBe MR.strings.unread
        presentation.bookmarkChildLabel shouldBe MR.strings.action_bookmark
        presentation.removeBookmarkChildLabel shouldBe MR.strings.action_remove_bookmark
        presentation.filterUnconsumedLabel shouldBe MR.strings.action_filter_unread
        presentation.childListTitle shouldBe MR.strings.chapters
        presentation.childCountPlural shouldBe MR.plurals.manga_num_chapters
        presentation.missingChildCountPlural shouldBe MR.plurals.missing_chapters
        presentation.childCountReasonLabel shouldBe MR.strings.possible_duplicates_reason_chapter_count
        presentation.childNumberDisplayLabel shouldBe MR.strings.display_mode_chapter
        presentation.childNumberSettingLabel shouldBe MR.strings.show_chapter_number
        presentation.deleteChildrenConfirmationLabel shouldBe MR.strings.confirm_delete_chapters
        presentation.noChildrenFoundLabel shouldBe MR.strings.no_chapters_error
        presentation.noNextChildLabel shouldBe MR.strings.no_next_chapter
        presentation.settingsTitle shouldBe MR.strings.chapter_settings
        presentation.setSettingsAsDefaultLabel shouldBe MR.strings.set_chapter_settings_as_default
        presentation.confirmSetSettingsAsDefaultLabel shouldBe MR.strings.confirm_set_chapter_settings
        presentation.alsoSetSettingsForLibraryLabel shouldBe MR.strings.also_set_chapter_settings_for_library
        presentation.settingsUpdatedLabel shouldBe MR.strings.chapter_settings_updated
        presentation.downloadAmountPlural shouldBe MR.plurals.download_amount
        presentation.downloadUnconsumedLabel shouldBe MR.strings.download_unread
        presentation.downloadNumberSortLabel shouldBe MR.strings.action_order_by_chapter_number
        presentation.intervalExpectedUpdateLabel shouldBe MR.strings.manga_interval_expected_update
        presentation.intervalExpectedUpdateNullLabel shouldBe MR.strings.manga_interval_expected_update_null
    }

    @Test
    fun `anime presentation uses existing anime labels and icons`() {
        val presentation = EntryType.ANIME.entryTypePresentation()

        presentation.displayNameLabel shouldBe MR.strings.entry_type_anime
        presentation.badgeIcon shouldBe Icons.Filled.PlayArrow
        presentation.smallIcon shouldBe R.drawable.ic_play_arrow_24dp
        presentation.coverOverlayIcon shouldNotBe null
        presentation.markAsConsumedLabel shouldBe MR.strings.action_mark_as_watched
        presentation.markAsUnconsumedLabel shouldBe MR.strings.action_mark_as_unwatched
        presentation.markPreviousAsConsumedLabel shouldBe MR.strings.action_mark_previous_as_watched
        presentation.unconsumedIndicatorLabel shouldBe MR.strings.action_filter_unwatched
        presentation.bookmarkChildLabel shouldBe MR.strings.action_bookmark_episode
        presentation.removeBookmarkChildLabel shouldBe MR.strings.action_remove_bookmark_episode
        presentation.filterUnconsumedLabel shouldBe MR.strings.action_filter_unwatched
        presentation.childListTitle shouldBe MR.strings.episodes
        presentation.childCountPlural shouldBe MR.plurals.anime_num_episodes
        presentation.missingChildCountPlural shouldBe MR.plurals.missing_episodes
        presentation.childCountReasonLabel shouldBe MR.strings.possible_duplicates_reason_episode_count
        presentation.childNumberDisplayLabel shouldBe MR.strings.display_mode_episode
        presentation.childNumberSettingLabel shouldBe MR.strings.show_episode_number
        presentation.deleteChildrenConfirmationLabel shouldBe MR.strings.confirm_delete_episodes
        presentation.noChildrenFoundLabel shouldBe MR.strings.anime_no_episodes
        presentation.noNextChildLabel shouldBe MR.strings.no_next_episode
        presentation.settingsTitle shouldBe MR.strings.episode_settings
        presentation.setSettingsAsDefaultLabel shouldBe MR.strings.set_episode_settings_as_default
        presentation.confirmSetSettingsAsDefaultLabel shouldBe MR.strings.confirm_set_episode_settings
        presentation.alsoSetSettingsForLibraryLabel shouldBe MR.strings.also_set_episode_settings_for_library
        presentation.settingsUpdatedLabel shouldBe MR.strings.episode_settings_updated
        presentation.downloadAmountPlural shouldBe MR.plurals.download_amount_episodes
        presentation.downloadUnconsumedLabel shouldBe MR.strings.download_unwatched
        presentation.downloadNumberSortLabel shouldBe MR.strings.action_order_by_episode_number
        presentation.intervalExpectedUpdateLabel shouldBe MR.strings.anime_interval_expected_update
        presentation.intervalExpectedUpdateNullLabel shouldBe MR.strings.anime_interval_expected_update_null
    }

    @Test
    fun `book presentation is explicit rather than generic fallback`() {
        val presentation = EntryType.BOOK.entryTypePresentation()

        presentation.displayNameLabel shouldBe MR.strings.entry_type_book
        presentation.badgeIcon shouldBe Icons.Outlined.Book
        presentation.smallIcon shouldBe R.drawable.ic_book_24dp
        presentation.coverOverlayIcon shouldNotBe null
        presentation.downloadBookmarkedSupported shouldBe false

        val mangaPresentation = EntryType.MANGA.entryTypePresentation()
        presentation.badgeIcon shouldNotBe mangaPresentation.badgeIcon
        presentation.smallIcon shouldNotBe mangaPresentation.smallIcon
    }

    @Test
    fun `fallback presentation uses neutral labels and icons`() {
        val presentation = (null as EntryType?).entryTypePresentation()

        presentation.displayNameLabel shouldBe MR.strings.entry_type_generic
        presentation.badgeIcon shouldBe Icons.Outlined.Info
        presentation.smallIcon shouldBe R.drawable.ic_info_24dp
        presentation.coverOverlayIcon shouldNotBe null
        presentation.markAsConsumedLabel shouldBe MR.strings.action_mark_as_consumed
        presentation.markAsUnconsumedLabel shouldBe MR.strings.action_mark_as_unconsumed
        presentation.markPreviousAsConsumedLabel shouldBe MR.strings.action_mark_previous_as_consumed
        presentation.unconsumedIndicatorLabel shouldBe MR.strings.action_filter_unconsumed
        presentation.bookmarkChildLabel shouldBe MR.strings.action_bookmark_item
        presentation.removeBookmarkChildLabel shouldBe MR.strings.action_remove_bookmark_item
        presentation.filterUnconsumedLabel shouldBe MR.strings.action_filter_unconsumed
        presentation.childListTitle shouldBe MR.strings.items
        presentation.childCountPlural shouldBe MR.plurals.entry_num_items
        presentation.missingChildCountPlural shouldBe MR.plurals.missing_items
        presentation.childCountReasonLabel shouldBe MR.strings.possible_duplicates_reason_item_count
        presentation.childNumberDisplayLabel shouldBe MR.strings.display_mode_item
        presentation.childNumberSettingLabel shouldBe MR.strings.show_item_number
        presentation.deleteChildrenConfirmationLabel shouldBe MR.strings.confirm_delete_items
        presentation.noChildrenFoundLabel shouldBe MR.strings.no_items_error
        presentation.noNextChildLabel shouldBe MR.strings.no_next_item
        presentation.settingsTitle shouldBe MR.strings.item_settings
        presentation.setSettingsAsDefaultLabel shouldBe MR.strings.set_item_settings_as_default
        presentation.confirmSetSettingsAsDefaultLabel shouldBe MR.strings.confirm_set_item_settings
        presentation.alsoSetSettingsForLibraryLabel shouldBe MR.strings.also_set_item_settings_for_library
        presentation.settingsUpdatedLabel shouldBe MR.strings.item_settings_updated
        presentation.downloadAmountPlural shouldBe MR.plurals.download_amount_items
        presentation.downloadUnconsumedLabel shouldBe MR.strings.download_unconsumed
        presentation.downloadNumberSortLabel shouldBe MR.strings.action_order_by_item_number
        presentation.intervalExpectedUpdateLabel shouldBe MR.strings.item_interval_expected_update
        presentation.intervalExpectedUpdateNullLabel shouldBe MR.strings.item_interval_expected_update_null
    }
}
