package eu.kanade.presentation.entry

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.PluralsResource
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.util.lang.toTimestampString
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA
import tachiyomi.presentation.core.i18n.stringResource
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds
import eu.kanade.presentation.util.toDurationString as durationToString

object EntryTypeIconDefaults {
    val InlineSize = 16.dp
    val CoverOverlaySize = 16.dp
}

data class EntryTypePresentation(
    val displayNameLabel: StringResource,
    val badgeIcon: ImageVector,
    @DrawableRes val smallIcon: Int,
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
    val downloadBookmarkedSupported: Boolean,
    val downloadNumberSortLabel: StringResource,
    val intervalExpectedUpdateLabel: StringResource,
    val intervalExpectedUpdateNullLabel: StringResource,
    val immersiveOpenLabel: StringResource,
    val immersiveOpenIcon: ImageVector,
)

fun EntryType?.entryTypePresentation(): EntryTypePresentation {
    return when (this) {
        EntryType.MANGA -> MangaEntryTypePresentation
        EntryType.ANIME -> AnimeEntryTypePresentation
        else -> GenericEntryTypePresentation
    }
}

fun Iterable<EntryType>.selectionEntryTypePresentation(): EntryTypePresentation {
    return toSet()
        .singleOrNull()
        .entryTypePresentation()
}

@Composable
fun EntryType.EntryTypeIcon(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = LocalContentColor.current,
) {
    Icon(
        imageVector = entryTypePresentation().badgeIcon,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
    )
}

@Composable
fun EntryType.InlineEntryTypeIndicator(
    modifier: Modifier = Modifier,
    size: Dp = EntryTypeIconDefaults.InlineSize,
    contentDescription: String? = null,
    tint: Color = LocalContentColor.current.copy(alpha = DISABLED_ALPHA),
) {
    EntryTypeIcon(
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint,
    )
}

@Composable
fun EntryType.coverTypeIndicatorOverlay(): (@Composable BoxScope.() -> Unit)? {
    val icon = entryTypePresentation().coverOverlayIcon ?: return null
    return {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .padding(4.dp)
                .size(EntryTypeIconDefaults.CoverOverlaySize)
                .align(Alignment.TopStart),
            tint = Color.White,
        )
    }
}

@Composable
fun EntryType.historySubtitle(
    childName: String,
    childNumber: Double,
    consumedAt: Date?,
    consumedDuration: Long,
): String {
    return when (this) {
        EntryType.ANIME -> {
            val context = LocalContext.current
            buildString {
                append(childName)
                val watchedAt = consumedAt?.toTimestampString()
                if (!watchedAt.isNullOrBlank()) {
                    append(" • ")
                    append(watchedAt)
                }
                if (consumedDuration > 0L) {
                    append(" • ")
                    append(
                        consumedDuration.milliseconds.durationToString(
                            context = context,
                            fallback = stringResource(MR.strings.not_applicable),
                        ),
                    )
                }
            }
        }
        EntryType.MANGA -> {
            if (childNumber > -1) {
                stringResource(
                    MR.strings.recent_manga_time,
                    formatChapterNumber(childNumber),
                    consumedAt?.toTimestampString() ?: "",
                )
            } else {
                consumedAt?.toTimestampString() ?: ""
            }
        }
    }
}

@Composable
fun EntryType.partialProgressLabel(position: Long): String? {
    if (position <= 0L) return null

    return when (this) {
        EntryType.MANGA -> stringResource(MR.strings.chapter_progress, position + 1)
        EntryType.ANIME -> stringResource(MR.strings.label_started)
    }
}

private val MangaEntryTypePresentation = EntryTypePresentation(
    displayNameLabel = MR.strings.entry_type_manga,
    badgeIcon = Icons.AutoMirrored.Outlined.MenuBook,
    smallIcon = R.drawable.ic_book_24dp,
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
    downloadBookmarkedSupported = true,
    downloadNumberSortLabel = MR.strings.action_order_by_chapter_number,
    intervalExpectedUpdateLabel = MR.strings.manga_interval_expected_update,
    intervalExpectedUpdateNullLabel = MR.strings.manga_interval_expected_update_null,
    immersiveOpenLabel = MR.strings.browse_manga_feed_open_in_reader,
    immersiveOpenIcon = Icons.AutoMirrored.Outlined.MenuBook,
)

private val AnimeEntryTypePresentation = EntryTypePresentation(
    displayNameLabel = MR.strings.entry_type_anime,
    badgeIcon = Icons.Filled.PlayArrow,
    smallIcon = R.drawable.ic_play_arrow_24dp,
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
    downloadBookmarkedSupported = false,
    downloadNumberSortLabel = MR.strings.action_order_by_episode_number,
    intervalExpectedUpdateLabel = MR.strings.anime_interval_expected_update,
    intervalExpectedUpdateNullLabel = MR.strings.anime_interval_expected_update_null,
    immersiveOpenLabel = MR.strings.browse_anime_feed_open_in_player,
    immersiveOpenIcon = Icons.Filled.PlayArrow,
)

private val GenericEntryTypePresentation = EntryTypePresentation(
    displayNameLabel = MR.strings.entry_type_generic,
    badgeIcon = Icons.Outlined.Info,
    smallIcon = R.drawable.ic_info_24dp,
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
    downloadBookmarkedSupported = false,
    downloadNumberSortLabel = MR.strings.action_order_by_item_number,
    intervalExpectedUpdateLabel = MR.strings.item_interval_expected_update,
    intervalExpectedUpdateNullLabel = MR.strings.item_interval_expected_update_null,
    immersiveOpenLabel = MR.strings.action_open,
    immersiveOpenIcon = Icons.AutoMirrored.Outlined.OpenInNew,
)
