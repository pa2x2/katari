package eu.kanade.tachiyomi.ui.video.player

import eu.kanade.tachiyomi.source.entry.VideoSubtitle

internal const val OFF_SUBTITLE_KEY = "off"

internal fun subtitleChoiceKey(subtitle: VideoSubtitle): String {
    return subtitle.key.ifBlank {
        listOf(subtitle.label, subtitle.language, subtitle.request.url).joinToString("|")
    }
}
