package mihon.entry.interactions.book

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryHistorySubtitlePresentation
import mihon.entry.interactions.EntryNotificationChildNumberPolicy
import mihon.entry.interactions.EntryPartialProgressPresentation
import mihon.entry.interactions.EntryTypePresentation
import mihon.entry.interactions.EntryTypePresentationProvider
import mihon.entry.interactions.EntryUpdateNotificationVocabulary
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
        bookmarkChildLabel = MR.strings.action_bookmark_item,
        removeBookmarkChildLabel = MR.strings.action_remove_bookmark_item,
        filterUnconsumedLabel = MR.strings.action_filter_unconsumed,
        childListTitle = MR.strings.items,
        childCountPlural = MR.plurals.entry_num_items,
        missingChildCountPlural = MR.plurals.missing_items,
        childCountReasonLabel = MR.strings.possible_duplicates_reason_item_count,
        childNumberDisplayLabel = MR.strings.display_mode_item,
        childNumberSettingLabel = MR.strings.show_item_number,
        deleteChildrenConfirmationLabel = MR.strings.confirm_delete_items,
        noChildrenFoundLabel = MR.strings.no_items_error,
        noNextChildLabel = MR.strings.no_next_item,
        settingsTitle = MR.strings.item_settings,
        setSettingsAsDefaultLabel = MR.strings.set_item_settings_as_default,
        confirmSetSettingsAsDefaultLabel = MR.strings.confirm_set_item_settings,
        alsoSetSettingsForLibraryLabel = MR.strings.also_set_item_settings_for_library,
        settingsUpdatedLabel = MR.strings.item_settings_updated,
        downloadAmountPlural = MR.plurals.download_amount_items,
        downloadUnconsumedLabel = MR.strings.download_unconsumed,
        downloadNumberSortLabel = MR.strings.action_order_by_item_number,
        intervalExpectedUpdateLabel = MR.strings.item_interval_expected_update,
        intervalExpectedUpdateNullLabel = MR.strings.item_interval_expected_update_null,
        immersiveOpenLabel = MR.strings.action_open,
        immersiveOpenIcon = Icons.Outlined.Book,
        historySubtitle = EntryHistorySubtitlePresentation.NameAndTimestamp,
        partialProgress = EntryPartialProgressPresentation.Fixed(MR.strings.label_started),
        updateNotification = EntryUpdateNotificationVocabulary(
            channelLabel = MR.strings.channel_new_items,
            summaryTitle = MR.strings.notification_new_items,
            summaryText = MR.plurals.notification_new_items_summary,
            childGeneric = MR.plurals.notification_items_generic,
            childSingle = MR.strings.notification_items_single,
            childSingleAndMore = MR.strings.notification_items_single_and_more,
            childMultiple = MR.strings.notification_items_multiple,
            childMultipleAndMore = MR.plurals.notification_items_multiple_and_more,
            viewChildrenLabel = MR.strings.action_view_items,
            numberPolicy = EntryNotificationChildNumberPolicy.NON_NEGATIVE,
            maxDisplayedNumbers = 5,
        ),
    )
}
