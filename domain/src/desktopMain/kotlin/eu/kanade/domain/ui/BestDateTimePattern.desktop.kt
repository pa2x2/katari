package eu.kanade.domain.ui

import com.ibm.icu.text.DateTimePatternGenerator
import java.util.Locale

// Android's DateFormat.getBestDateTimePattern is this ICU call, so both platforms format times alike
internal actual fun bestDateTimePattern(locale: Locale, skeleton: String): String =
    DateTimePatternGenerator.getInstance(locale).getBestPattern(skeleton)
