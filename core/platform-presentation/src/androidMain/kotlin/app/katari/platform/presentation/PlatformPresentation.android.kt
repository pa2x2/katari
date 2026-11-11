package app.katari.platform.presentation

import app.katari.icons.platformsymbols.PlatformSymbols
import app.katari.icons.platformsymbols.rounded.PhonelinkErase
import app.katari.icons.platformsymbols.rounded.Smartphone
import app.katari.icons.platformsymbols.rounded.TouchApp
import tachiyomi.i18n.*

actual val platformPresentation = PlatformPresentation(
    thisDeviceIcon = PlatformSymbols.Rounded.Smartphone,
    thisDeviceRemovedIcon = PlatformSymbols.Rounded.PhonelinkErase,
    pointerIcon = PlatformSymbols.Rounded.TouchApp,
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
    downloadOnlyOnUnmeteredLabel = MR.strings.connected_to_wifi,
    downloadOnlyOnUnmeteredSummary = null,
    translateOnlyOnUnmeteredLabel = PlatformStrings.pref_translate_only_over_wifi,
    translateWaitsForUnmeteredLabel = PlatformStrings.translate_waiting_for_wifi,
    loadAheadOnlyOnUnmeteredLabel = PlatformStrings.pref_load_ahead_only_over_wifi,
    loadAheadOnlyOnUnmeteredSummary = PlatformStrings.pref_load_ahead_only_over_wifi_summary,
    syncFilesOnMeteredLabel = PlatformStrings.sync_use_mobile_data,
    syncFilesOnMeteredSummary = null,
    syncChangesOnMeteredLabel = PlatformStrings.sync_changes_on_mobile_data,
    syncChangesOnMeteredSummary = PlatformStrings.sync_changes_on_mobile_data_summary,
    syncFirstDownloadWaitsLabel = PlatformStrings.sync_status_first_download_wifi,
    syncChangesWaitLabel = PlatformStrings.sync_status_changes_wifi,
    syncUnsentWaitingNote = PlatformStrings.sync_unsent_waiting_for_wifi,
    syncFilesWaitingPlural = PlatformPlurals.sync_files_waiting_for_wifi,
    dumpCrashLogsLabel = PlatformStrings.pref_dump_crash_logs,
    syncFailedHelp = PlatformStrings.sync_failed_help,
    securitySettingsSummary = PlatformStrings.pref_security_summary,
    monthlyRecapSummary = PlatformStrings.statistics_monthly_recap_summary,
)
