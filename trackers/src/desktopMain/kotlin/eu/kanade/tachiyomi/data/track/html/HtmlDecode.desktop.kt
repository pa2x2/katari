package eu.kanade.tachiyomi.data.track.html

import org.jsoup.Jsoup

// Whole text keeps the snippet's own line breaks and turns <br> into one, as Android's Html.fromHtml does.
internal actual fun String.htmlDecode(): String = Jsoup.parseBodyFragment(this).wholeText()
