package app.katari.platform.presentation

import androidx.compose.ui.graphics.vector.ImageVector
import dev.icerock.moko.resources.PluralsResource
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
    /**
     * Names the network that work kept off costly connections waits for: Wi-Fi where the platform can tell it, and
     * otherwise a network the system doesn't call metered.
     */
    val downloadOnlyOnUnmeteredLabel: StringResource,
    val downloadOnlyOnUnmeteredSummary: StringResource?,
    val translateOnlyOnUnmeteredLabel: StringResource,
    val translateWaitsForUnmeteredLabel: StringResource,
    val loadAheadOnlyOnUnmeteredLabel: StringResource,
    val loadAheadOnlyOnUnmeteredSummary: StringResource,
    val syncFilesOnMeteredLabel: StringResource,
    val syncFilesOnMeteredSummary: StringResource?,
    val syncChangesOnMeteredLabel: StringResource,
    val syncChangesOnMeteredSummary: StringResource,
    val syncFirstDownloadWaitsLabel: StringResource,
    val syncChangesWaitLabel: StringResource,
    /** Follows a count of unsent changes. */
    val syncUnsentWaitingNote: StringResource,
    val syncFilesWaitingPlural: PluralsResource,
    /** Names the action that hands over the crash logs: sharing them, or saving them to a file. */
    val dumpCrashLogsLabel: StringResource,
    val syncFailedHelp: StringResource,
    val securitySettingsSummary: StringResource,
    val monthlyRecapSummary: StringResource,
    /** Names the parts of a page that turn it or show the menu when pressed. */
    val pageZonesLabel: StringResource,
    val invertPageZonesLabel: StringResource,
    val showPageZonesOverlayLabel: StringResource,
    val pageActionsOnLongPressLabel: StringResource,
    /**
     * Names the chapter row action a swipe towards the start runs. A mouse has nothing to swipe with, so there the
     * two actions are named as the buttons the row shows under the pointer, in their order.
     */
    val chapterSwipeToStartLabel: StringResource,
    val chapterSwipeToEndLabel: StringResource,
    val browseLongPressPriorityLabel: StringResource,
    val longPressActionsLabel: StringResource,
    /** Takes the source's name. */
    val longPressActionsForSourceLabel: StringResource,
    val triStateChipHint: StringResource,
    /** Null where the platform asks no permission to install extensions. */
    val installPermissionWarning: StringResource?,
    /** Null where the player doesn't set the screen's brightness. */
    val rememberBrightnessSummary: StringResource?,
    /** Null where the platform can't tell Wi-Fi from other networks. */
    val wifiOnlyLabel: StringResource?,
    /** Pictures the reading mode that follows the reader's default. */
    val defaultReadingModeIcon: ImageVector,
    /** Pictures the network that work kept off costly connections waits for. */
    val unmeteredNetworkIcon: ImageVector,
)

expect val platformPresentation: PlatformPresentation
