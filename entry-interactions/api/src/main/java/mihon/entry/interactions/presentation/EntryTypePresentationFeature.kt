@file:JvmName("EntryTypePresentationModels")

package mihon.entry.interactions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.graphics.vector.ImageVector
import dev.icerock.moko.resources.PluralsResource
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.i18n.MR

/** Type-owned vocabulary and imagery. These values describe a type; they never authorize product behavior. */
data class EntryTypePresentation(
    val displayNameLabel: StringResource,
    val badgeIcon: ImageVector,
    val coverOverlayIcon: ImageVector?,
    val markAsConsumedLabel: StringResource,
    val markAsUnconsumedLabel: StringResource,
    val markPreviousAsConsumedLabel: StringResource,
    val unconsumedIndicatorLabel: StringResource,
    val bookmarkChildLabel: StringResource,
    val removeBookmarkChildLabel: StringResource,
    val filterUnconsumedLabel: StringResource,
    val childListTitle: StringResource,
    val childCountPlural: PluralsResource,
    val missingChildCountPlural: PluralsResource,
    val childCountReasonLabel: StringResource,
    val childNumberDisplayLabel: StringResource,
    val childNumberSettingLabel: StringResource,
    val deleteChildrenConfirmationLabel: StringResource,
    val noChildrenFoundLabel: StringResource,
    val noNextChildLabel: StringResource,
    val settingsTitle: StringResource,
    val setSettingsAsDefaultLabel: StringResource,
    val confirmSetSettingsAsDefaultLabel: StringResource,
    val alsoSetSettingsForLibraryLabel: StringResource,
    val settingsUpdatedLabel: StringResource,
    val downloadAmountPlural: PluralsResource,
    val downloadUnconsumedLabel: StringResource,
    val downloadNumberSortLabel: StringResource,
    val intervalExpectedUpdateLabel: StringResource,
    val intervalExpectedUpdateNullLabel: StringResource,
    val immersiveOpenLabel: StringResource,
    val immersiveOpenIcon: ImageVector,
    val historySubtitle: EntryHistorySubtitlePresentation,
    val partialProgress: EntryPartialProgressPresentation,
    val updateNotification: EntryUpdateNotificationVocabulary,
)

sealed interface EntryHistorySubtitlePresentation {
    data class NumberAndTimestamp(val label: StringResource) : EntryHistorySubtitlePresentation
    data object NameTimestampAndDuration : EntryHistorySubtitlePresentation
    data object NameAndTimestamp : EntryHistorySubtitlePresentation
}

sealed interface EntryPartialProgressPresentation {
    data class NumberedPosition(val label: StringResource) : EntryPartialProgressPresentation
    data class Fixed(val label: StringResource) : EntryPartialProgressPresentation
}

/** Vocabulary for library-update notifications; routing, identity, actions, and applicability remain notification-owned. */
data class EntryUpdateNotificationVocabulary(
    val channelLabel: StringResource,
    val summaryTitle: StringResource,
    val summaryText: PluralsResource,
    val childGeneric: PluralsResource,
    val childSingle: StringResource,
    val childSingleAndMore: StringResource,
    val childMultiple: StringResource,
    val childMultipleAndMore: PluralsResource,
    val viewChildrenLabel: StringResource,
    val numberPolicy: EntryNotificationChildNumberPolicy,
    val maxDisplayedNumbers: Int,
)

enum class EntryNotificationChildNumberPolicy {
    RECOGNIZED_ONLY,
    NON_NEGATIVE,
}

sealed interface EntryTypePresentationResult {
    val presentation: EntryTypePresentation

    data class Contributed(
        val type: EntryType,
        override val presentation: EntryTypePresentation,
    ) : EntryTypePresentationResult

    /** Explicit emergency vocabulary. The requested type is retained so absence remains observable. */
    data class Generic(
        val type: EntryType?,
        override val presentation: EntryTypePresentation,
    ) : EntryTypePresentationResult
}

/** Application-facing projection boundary. Provider presence selects presentation and no behavioral capability. */
interface EntryTypePresentationFeature {
    val genericPresentation: EntryTypePresentation

    fun presentation(type: EntryType?): EntryTypePresentationResult
}

/** Explicit neutral vocabulary for mixed/no-type surfaces; never a fallback for a concrete contributed type. */
val genericEntryTypePresentation = EntryTypePresentation(
    displayNameLabel = MR.strings.entry_type_generic,
    badgeIcon = Icons.Outlined.Info,
    coverOverlayIcon = Icons.Outlined.Info,
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
    immersiveOpenIcon = Icons.AutoMirrored.Outlined.OpenInNew,
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
