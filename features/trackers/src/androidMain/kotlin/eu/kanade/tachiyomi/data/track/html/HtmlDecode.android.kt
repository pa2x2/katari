package eu.kanade.tachiyomi.data.track.html

import eu.kanade.tachiyomi.util.lang.htmlDecode as androidHtmlDecode

internal actual fun String.htmlDecode(): String = androidHtmlDecode()
