package mihon.entry.interactions.anime.stream

import eu.kanade.tachiyomi.source.entry.VideoSubtitle
import mihon.video.engine.spi.tracks.subtitleChoiceKey

/**
 * How a title's subtitle is picked when nobody picks one for the episode, shared by the player and downloads so that a
 * download keeps the subtitle streaming shows. A saved key is [OFF_SUBTITLE_KEY], a [subtitlePreferenceKey], or the
 * [subtitleChoiceKey] older releases saved; with none saved, the source's default subtitle shows, else its first.
 */
internal const val OFF_SUBTITLE_KEY = "off"

/** The key a subtitle is remembered by across episodes: its label and language, which its URL and key may not share. */
internal fun subtitlePreferenceKey(subtitle: VideoSubtitle): String {
    val stableKey = listOfNotNull(
        subtitle.label.trim().takeIf(String::isNotBlank),
        subtitle.language?.trim()?.takeIf(String::isNotBlank),
    ).joinToString("|").ifBlank {
        subtitle.key.ifBlank { subtitleChoiceKey(subtitle) }
    }
    return "$EXTERNAL_SUBTITLE_PREFERENCE_PREFIX$stableKey"
}

internal fun sourceDefaultSubtitle(subtitles: List<VideoSubtitle>): VideoSubtitle? =
    subtitles.firstOrNull(VideoSubtitle::isDefault) ?: subtitles.firstOrNull()

/** The subtitle among [subtitles] that the saved [key] names, null when none does. */
internal fun savedSubtitle(key: String, subtitles: List<VideoSubtitle>): VideoSubtitle? =
    subtitles.firstOrNull { subtitlePreferenceKey(it) == key || subtitleChoiceKey(it) == key }

/** The subtitle shown for the title's saved [key], null for none; a key no subtitle matches falls back to the default. */
internal fun automaticSubtitle(key: String?, subtitles: List<VideoSubtitle>): VideoSubtitle? = when (key) {
    null -> sourceDefaultSubtitle(subtitles)
    OFF_SUBTITLE_KEY -> null
    else -> savedSubtitle(key, subtitles) ?: sourceDefaultSubtitle(subtitles)
}

private const val EXTERNAL_SUBTITLE_PREFERENCE_PREFIX = "external:"
