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
)
