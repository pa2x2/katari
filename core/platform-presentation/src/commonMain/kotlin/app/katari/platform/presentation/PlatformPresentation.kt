package app.katari.platform.presentation

import androidx.compose.ui.graphics.vector.ImageVector

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
)

expect val platformPresentation: PlatformPresentation
