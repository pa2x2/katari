package eu.kanade.presentation.more.stats.components

import android.icu.text.ListFormatter
import java.util.Locale

internal actual fun localizedList(items: List<String>, locale: Locale): String =
    ListFormatter.getInstance(locale).format(items)
