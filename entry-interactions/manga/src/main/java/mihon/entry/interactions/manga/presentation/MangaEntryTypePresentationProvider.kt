package mihon.entry.interactions.manga

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryHistorySubtitlePresentation
import mihon.entry.interactions.EntryNotificationChildNumberPolicy
import mihon.entry.interactions.EntryPartialProgressPresentation
import mihon.entry.interactions.EntryTypePresentation
import mihon.entry.interactions.EntryTypePresentationProvider
import mihon.entry.interactions.EntryUpdateNotificationVocabulary
import tachiyomi.i18n.MR

internal object MangaEntryTypePresentationProvider : EntryTypePresentationProvider {
    override val type = EntryType.MANGA
    override val presentation = EntryTypePresentation(
        displayNameLabel = MR.strings.entry_type_manga,
        badgeIcon = Icons.AutoMirrored.Outlined.MenuBook,
        coverOverlayIcon = null,
        markAsConsumedLabel = MR.strings.action_mark_as_read,
        markAsUnconsumedLabel = MR.strings.action_mark_as_unread,
        markPreviousAsConsumedLabel = MR.strings.action_mark_previous_as_read,
        unconsumedIndicatorLabel = MR.strings.unread,
        bookmarkChildLabel = MR.strings.action_bookmark,
        removeBookmarkChildLabel = MR.strings.action_remove_bookmark,
        filterUnconsumedLabel = MR.strings.action_filter_unread,
        childListTitle = MR.strings.chapters,
        childCountPlural = MR.plurals.manga_num_chapters,
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
        downloadUnconsumedLabel = MR.strings.download_unread,
        downloadNumberSortLabel = MR.strings.action_order_by_chapter_number,
        intervalExpectedUpdateLabel = MR.strings.manga_interval_expected_update,
        intervalExpectedUpdateNullLabel = MR.strings.manga_interval_expected_update_null,
        immersiveOpenLabel = MR.strings.browse_manga_feed_open_in_reader,
        immersiveOpenIcon = Icons.AutoMirrored.Outlined.MenuBook,
        historySubtitle = EntryHistorySubtitlePresentation.NumberAndTimestamp(MR.strings.recent_manga_time),
        partialProgress = EntryPartialProgressPresentation.NumberedPosition(MR.strings.chapter_progress),
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
            numberPolicy = EntryNotificationChildNumberPolicy.RECOGNIZED_ONLY,
            maxDisplayedNumbers = 5,
        ),
    )
}
