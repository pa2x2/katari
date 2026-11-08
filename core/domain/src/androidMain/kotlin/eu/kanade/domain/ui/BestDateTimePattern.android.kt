package eu.kanade.domain.ui

import android.text.format.DateFormat
import java.util.Locale

actual fun bestDateTimePattern(locale: Locale, skeleton: String): String =
    DateFormat.getBestDateTimePattern(locale, skeleton)
