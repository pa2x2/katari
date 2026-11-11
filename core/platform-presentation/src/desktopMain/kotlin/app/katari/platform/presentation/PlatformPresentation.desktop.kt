package app.katari.platform.presentation

import app.katari.icons.platformsymbols.PlatformSymbols
import app.katari.icons.platformsymbols.rounded.Computer
import app.katari.icons.platformsymbols.rounded.ComputerCancel
import app.katari.icons.platformsymbols.rounded.Mouse
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
)
