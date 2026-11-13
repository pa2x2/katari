package app.katari.platform.presentation

import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Public
import app.katari.icons.platformsymbols.PlatformSymbols
import app.katari.icons.platformsymbols.rounded.Computer
import app.katari.icons.platformsymbols.rounded.ComputerCancel
import app.katari.icons.platformsymbols.rounded.DisplaySettings
import app.katari.icons.platformsymbols.rounded.Mouse
import app.katari.icons.platformsymbols.rounded.TrackpadInput2
import tachiyomi.i18n.*

actual val platformPresentation = PlatformPresentation(
    thisDeviceIcon = PlatformSymbols.Rounded.Computer,
    thisDeviceRemovedIcon = PlatformSymbols.Rounded.ComputerCancel,
    pointerIcon = PlatformSymbols.Rounded.Mouse,
    emptyCategoriesLabel = PlatformStrings.information_empty_category,
    downloadOptionsFailedLabel = PlatformStrings.download_options_failed,
    downloadQueueSizeWarning = PlatformStrings.download_queue_size_warning,
    imageUnavailableLabel = PlatformStrings.book_document_image_unavailable,
    backupLockedProfileLabel = PlatformStrings.backup_profile_locked,
    retryMergeMaintenanceSummary = PlatformStrings.pref_retry_merge_maintenance_summary,
    doubleTapZoomLabel = PlatformStrings.pref_double_tap_zoom,
    doubleTapAnimationSpeedLabel = PlatformStrings.pref_double_tap_anim_speed,
    translateEveryTextSummary = PlatformStrings.pref_page_text_translation_overlay_summary,
    seekStepSummary = PlatformStrings.pref_anime_seek_step_summary,
    recapStartLabel = PlatformStrings.statistics_recap_tap_to_start,
    recapNotificationText = PlatformStrings.statistics_recap_notification_text,
    yearRecapNotificationText = PlatformStrings.statistics_year_recap_notification_text,
    cloudflareHelpLabel = PlatformStrings.information_cloudflare_help,
    downloadOnlyOnUnmeteredLabel = MR.strings.network_not_metered,
    downloadOnlyOnUnmeteredSummary = PlatformStrings.pref_download_only_unmetered_summary,
    translateOnlyOnUnmeteredLabel = MR.strings.network_not_metered,
    translateWaitsForUnmeteredLabel = PlatformStrings.translate_waiting_for_unmetered,
    loadAheadOnlyOnUnmeteredLabel = PlatformStrings.pref_load_ahead_only_unmetered,
    loadAheadOnlyOnUnmeteredSummary = PlatformStrings.pref_load_ahead_only_unmetered_summary,
    syncFilesOnMeteredLabel = PlatformStrings.sync_use_metered,
    syncFilesOnMeteredSummary = PlatformStrings.sync_use_metered_summary,
    syncChangesOnMeteredLabel = PlatformStrings.sync_changes_on_metered,
    syncChangesOnMeteredSummary = PlatformStrings.sync_changes_on_metered_summary,
    syncFirstDownloadWaitsLabel = PlatformStrings.sync_status_first_download_unmetered,
    syncChangesWaitLabel = PlatformStrings.sync_status_changes_unmetered,
    syncUnsentWaitingNote = PlatformStrings.sync_unsent_waiting_for_unmetered,
    syncFilesWaitingPlural = PlatformPlurals.sync_files_waiting_for_unmetered,
    dumpCrashLogsLabel = PlatformStrings.pref_dump_crash_logs,
    syncFailedHelp = PlatformStrings.sync_failed_help,
    syncTokenUnreadableLabel = PlatformStrings.sync_status_token_unreadable,
    syncTokenUnreadableHelp = PlatformStrings.sync_token_unreadable_help,
    syncTokenUnreadableAction = PlatformStrings.sync_action_unlock_keyring,
    syncMarkHint = PlatformStrings.sync_mark_hint,
    securitySettingsSummary = PlatformStrings.pref_security_summary,
    monthlyRecapSummary = PlatformStrings.statistics_monthly_recap_summary,
    pageZonesLabel = PlatformStrings.pref_viewer_nav,
    invertPageZonesLabel = PlatformStrings.pref_read_with_tapping_inverted,
    showPageZonesOverlayLabel = PlatformStrings.pref_show_navigation_mode,
    pageActionsOnLongPressLabel = null,
    chapterSwipeToStartLabel = PlatformStrings.pref_chapter_swipe_start,
    chapterSwipeToEndLabel = PlatformStrings.pref_chapter_swipe_end,
    browseLongPressPriorityLabel = PlatformStrings.pref_browse_long_press_action,
    longPressActionsLabel = PlatformStrings.pref_browse_long_press_action_open_settings,
    longPressActionsForSourceLabel = PlatformStrings.browse_feed_long_press_actions_for_source,
    triStateChipHint = PlatformStrings.filter_tristate_chip_hint,
    autoScrollSummary = PlatformStrings.pref_auto_scroll_summary,
    installPermissionWarning = null,
    rememberBrightnessSummary = null,
    wifiOnlyLabel = null,
    defaultReadingModeIcon = PlatformSymbols.Rounded.DisplaySettings,
    unmeteredNetworkIcon = MaterialSymbols.Rounded.Public,
    touchpadSettings = SettingsEntryPresentation(
        title = PlatformStrings.pref_category_touchpad,
        summary = PlatformStrings.pref_touchpad_summary,
        icon = PlatformSymbols.Rounded.TrackpadInput2,
    ),
)
