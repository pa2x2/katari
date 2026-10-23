package mihon.video.engine.spi.tracks

import eu.kanade.tachiyomi.source.entry.VideoSubtitle

/**
 * The key an external subtitle is chosen, remembered and downloaded by. An engine publishes the text track it loads
 * for [subtitle] with a `Format.id` of `<n>:<key>`, the form ExoPlayer's merging media source gives it, which is how
 * the player finds the track of a chosen subtitle.
 */
fun subtitleChoiceKey(subtitle: VideoSubtitle): String {
    return subtitle.key.ifBlank {
        listOf(subtitle.label, subtitle.language, subtitle.request.url).joinToString("|")
    }
}
