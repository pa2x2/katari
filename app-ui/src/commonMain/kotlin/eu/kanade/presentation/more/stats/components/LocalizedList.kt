package eu.kanade.presentation.more.stats.components

import java.util.Locale

/** [items] joined as [locale] writes a list, such as "Chapters, episodes and pages". */
internal expect fun localizedList(items: List<String>, locale: Locale): String
