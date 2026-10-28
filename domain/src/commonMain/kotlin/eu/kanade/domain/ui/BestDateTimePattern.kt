package eu.kanade.domain.ui

import java.util.Locale

/** The [locale]'s pattern for the fields in [skeleton] (`"j"`, `"Hm"`, …), ordered and punctuated as it writes them. */
expect fun bestDateTimePattern(locale: Locale, skeleton: String): String
