package eu.kanade.presentation.util

import android.text.format.DateFormat
import mihon.core.platform.PlatformContext

internal actual fun PlatformContext.is24HourSystem(): Boolean = DateFormat.is24HourFormat(this)
