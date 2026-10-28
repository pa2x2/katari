package eu.kanade.presentation.util

import mihon.core.platform.PlatformContext

/** Whether the system shows times on the 24-hour clock, which the Time format setting's "Follow system" choice reads. */
internal expect fun PlatformContext.is24HourSystem(): Boolean
