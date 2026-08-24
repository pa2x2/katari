package mihon.entry.interactions.book.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.presentation.EntryHistorySubtitlePresentation
import mihon.entry.interactions.presentation.EntryNotificationChildNumberPolicy
import mihon.entry.interactions.presentation.EntryPartialProgressPresentation
import mihon.entry.interactions.presentation.EntryTypePresentation
import mihon.entry.interactions.presentation.EntryUpdateNotificationVocabulary
import mihon.entry.interactions.runtime.EntryTypePresentationProvider
import tachiyomi.i18n.MR

internal object BookEntryTypePresentationProvider : EntryTypePresentationProvider {
    override val type = EntryType.BOOK
    override val presentation = EntryTypePresentation(
        displayNameLabel = MR.strings.entry_type_book,
        badgeIcon = Icons.Outlined.Book,
        coverOverlayIcon = Icons.Outlined.Book,
        markAsConsumedLabel = MR.strings.action_mark_as_consumed,
        markAsUnconsumedLabel = MR.strings.action_mark_as_unconsumed,
        markPreviousAsConsumedLabel = MR.strings.action_mark_previous_as_consumed,
        unconsumedIndicatorLabel = MR.strings.action_filter_unconsumed,
        bookmarkChildLabel = MR.strings.action_bookmark,
        removeBookmarkChildLabel = MR.strings.action_remove_bookmark,
        filterUnconsumedLabel = MR.strings.action_filter_unconsumed,
        childListTitle = MR.strings.chapters,
        childCountPlural = MR.plurals.manga_num_chapters,
        completedChildCountPlural = MR.plurals.activity_completed_chapters,
        missingChildCountPlural = MR.plurals.missing_chapters,
        childCountReasonLabel = MR.strings.possible_duplicates_reason_chapter_count,
        childNumberDisplayLabel = MR.strings.display_mode_chapter,
        childNumberSettingLabel = MR.strings.show_chapter_number,
        deleteChildrenConfirmationLabel = MR.strings.confirm_delete_chapters,
        noChildrenFoundLabel = MR.strings.no_chapters_error,
        noNextChildLabel = MR.strings.no_next_chapter,
        settingsTitle = MR.strings.chapter_settings,
        setSettingsAsDefaultLabel = MR.strings.set_chapter_settings_as_default,
        confirmSetSettingsAsDefaultLabel = MR.strings.confirm_set_chapter_settings,
        alsoSetSettingsForLibraryLabel = MR.strings.also_set_chapter_settings_for_library,
        settingsUpdatedLabel = MR.strings.chapter_settings_updated,
        downloadAmountPlural = MR.plurals.download_amount,
        downloadUnconsumedLabel = MR.strings.download_unconsumed,
        downloadNumberSortLabel = MR.strings.action_order_by_chapter_number,
        intervalExpectedUpdateLabel = MR.strings.manga_interval_expected_update,
        intervalExpectedUpdateNullLabel = MR.strings.manga_interval_expected_update_null,
        immersiveOpenLabel = MR.strings.action_open,
        immersiveOpenIcon = Icons.Outlined.Book,
        historySubtitle = EntryHistorySubtitlePresentation.NameAndTimestamp,
        partialProgress = EntryPartialProgressPresentation.Fixed(MR.strings.label_started),
        updateNotification = EntryUpdateNotificationVocabulary(
            channelLabel = MR.strings.channel_new_chapters,
            summaryTitle = MR.strings.notification_new_chapters,
            summaryText = MR.plurals.notification_new_chapters_summary,
            childGeneric = MR.plurals.notification_chapters_generic,
            childSingle = MR.strings.notification_chapters_single,
            childSingleAndMore = MR.strings.notification_chapters_single_and_more,
            childMultiple = MR.strings.notification_chapters_multiple,
            childMultipleAndMore = MR.plurals.notification_chapters_multiple_and_more,
            viewChildrenLabel = MR.strings.action_view_chapters,
            numberPolicy = EntryNotificationChildNumberPolicy.NON_NEGATIVE,
            maxDisplayedNumbers = 5,
        ),
    )
}
