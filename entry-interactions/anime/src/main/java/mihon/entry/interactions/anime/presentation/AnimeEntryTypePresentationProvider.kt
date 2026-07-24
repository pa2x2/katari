package mihon.entry.interactions.anime

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryHistorySubtitlePresentation
import mihon.entry.interactions.EntryNotificationChildNumberPolicy
import mihon.entry.interactions.EntryPartialProgressPresentation
import mihon.entry.interactions.EntryTypePresentation
import mihon.entry.interactions.EntryTypePresentationProvider
import mihon.entry.interactions.EntryUpdateNotificationVocabulary
import tachiyomi.i18n.MR

internal object AnimeEntryTypePresentationProvider : EntryTypePresentationProvider {
    override val type = EntryType.ANIME
    override val presentation = EntryTypePresentation(
        displayNameLabel = MR.strings.entry_type_anime,
        badgeIcon = Icons.Filled.PlayArrow,
        coverOverlayIcon = Icons.Filled.PlayArrow,
        markAsConsumedLabel = MR.strings.action_mark_as_watched,
        markAsUnconsumedLabel = MR.strings.action_mark_as_unwatched,
        markPreviousAsConsumedLabel = MR.strings.action_mark_previous_as_watched,
        unconsumedIndicatorLabel = MR.strings.action_filter_unwatched,
        bookmarkChildLabel = MR.strings.action_bookmark_episode,
        removeBookmarkChildLabel = MR.strings.action_remove_bookmark_episode,
        filterUnconsumedLabel = MR.strings.action_filter_unwatched,
        childListTitle = MR.strings.episodes,
        childCountPlural = MR.plurals.anime_num_episodes,
        missingChildCountPlural = MR.plurals.missing_episodes,
        childCountReasonLabel = MR.strings.possible_duplicates_reason_episode_count,
        childNumberDisplayLabel = MR.strings.display_mode_episode,
        childNumberSettingLabel = MR.strings.show_episode_number,
        deleteChildrenConfirmationLabel = MR.strings.confirm_delete_episodes,
        noChildrenFoundLabel = MR.strings.anime_no_episodes,
        noNextChildLabel = MR.strings.no_next_episode,
        settingsTitle = MR.strings.episode_settings,
        setSettingsAsDefaultLabel = MR.strings.set_episode_settings_as_default,
        confirmSetSettingsAsDefaultLabel = MR.strings.confirm_set_episode_settings,
        alsoSetSettingsForLibraryLabel = MR.strings.also_set_episode_settings_for_library,
        settingsUpdatedLabel = MR.strings.episode_settings_updated,
        downloadAmountPlural = MR.plurals.download_amount_episodes,
        downloadUnconsumedLabel = MR.strings.download_unwatched,
        downloadNumberSortLabel = MR.strings.action_order_by_episode_number,
        intervalExpectedUpdateLabel = MR.strings.anime_interval_expected_update,
        intervalExpectedUpdateNullLabel = MR.strings.anime_interval_expected_update_null,
        immersiveOpenLabel = MR.strings.browse_anime_feed_open_in_player,
        immersiveOpenIcon = Icons.Filled.PlayArrow,
        historySubtitle = EntryHistorySubtitlePresentation.NameTimestampAndDuration,
        partialProgress = EntryPartialProgressPresentation.Fixed(MR.strings.label_started),
        updateNotification = EntryUpdateNotificationVocabulary(
            channelLabel = MR.strings.channel_new_episodes,
            summaryTitle = MR.strings.notification_new_episodes,
            summaryText = MR.plurals.notification_new_episodes_summary,
            childGeneric = MR.plurals.notification_episodes_generic,
            childSingle = MR.strings.notification_episodes_single,
            childSingleAndMore = MR.strings.notification_episodes_single_and_more,
            childMultiple = MR.strings.notification_episodes_multiple,
            childMultipleAndMore = MR.plurals.notification_episodes_multiple_and_more,
            viewChildrenLabel = MR.strings.action_view_episodes,
            numberPolicy = EntryNotificationChildNumberPolicy.NON_NEGATIVE,
            maxDisplayedNumbers = 5,
        ),
    )
}
