package eu.kanade.tachiyomi.ui.video.player.recovery

/**
 * Where an episode that couldn't open is fixed outside the player: the extensions, for a source that isn't installed
 * or doesn't load; the entry, whose episode list a refresh brings up to date; the source's settings, which may offer
 * a stream in place of a torrent.
 */
internal sealed interface VideoProblemAction {
    data object OpenExtensions : VideoProblemAction

    data class OpenEntry(val entryId: Long) : VideoProblemAction

    data class OpenSourceSettings(val sourceId: Long) : VideoProblemAction
}
