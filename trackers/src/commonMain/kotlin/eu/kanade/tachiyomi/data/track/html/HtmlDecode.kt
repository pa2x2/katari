package eu.kanade.tachiyomi.data.track.html

/** The text of a tracker's HTML snippet, such as a series description, with its tags dropped and entities decoded. */
internal expect fun String.htmlDecode(): String
