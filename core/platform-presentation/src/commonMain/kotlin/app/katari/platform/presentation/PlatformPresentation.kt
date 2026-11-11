package app.katari.platform.presentation

import androidx.compose.ui.graphics.vector.ImageVector
import dev.icerock.moko.resources.StringResource

/**
 * The wording and imagery that differ between the platforms the app runs on. Shared code reads them here instead of
 * naming a phrase or picture that fits one platform only: such strings exist only in that platform's string files and
 * such icons only in this module, so neither can be named from shared code.
 */
data class PlatformPresentation(
    val thisDeviceIcon: ImageVector,
    /** Marks this device as removed from its account. */
    val thisDeviceRemovedIcon: ImageVector,
    /** Pictures what the app is pointed at and pressed with: a finger or a mouse. */
    val pointerIcon: ImageVector,
    val emptyCategoriesLabel: StringResource,
    val downloadOptionsFailedLabel: StringResource,
    /** Takes the app's name. */
    val downloadQueueSizeWarning: StringResource,
    /** Stands in for a picture that didn't load, and says how to try again. */
    val imageUnavailableLabel: StringResource,
    val backupLockedProfileLabel: StringResource,
    /** Takes the pending and the failed counts. */
    val retryMergeMaintenanceSummary: StringResource,
    val doubleTapZoomLabel: StringResource,
    val doubleTapAnimationSpeedLabel: StringResource,
    val translateEveryTextSummary: StringResource,
    /** Takes the seek step, and names what seeks by it. */
    val seekStepSummary: StringResource,
    val recapStartLabel: StringResource,
    /** Takes the month's active days. */
    val recapNotificationText: StringResource,
    /** Takes the year's total time. */
    val yearRecapNotificationText: StringResource,
    val cloudflareHelpLabel: StringResource,
)

expect val platformPresentation: PlatformPresentation
