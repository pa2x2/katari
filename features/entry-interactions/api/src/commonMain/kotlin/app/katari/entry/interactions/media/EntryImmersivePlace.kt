package app.katari.entry.interactions.media

import app.katari.entry.interactions.navigation.EntryOpenOptions

/** Where the media shown in the feed is. The feed keeps it to itself; opening the episode or chapter carries on there. */
sealed interface EntryImmersivePlace {
    data class ImagePage(val pageIndex: Int) : EntryImmersivePlace

    data class Playback(val positionMs: Long) : EntryImmersivePlace
}

/**
 * The place of one page's media, read when its episode or chapter opens. Its renderer [track]s it with a reader of the
 * place as it is at that moment, since a playing clip's position moves without telling anyone.
 */
class EntryImmersivePlaceTracker {
    private var reader: (() -> EntryImmersivePlace)? = null

    fun track(reader: () -> EntryImmersivePlace) {
        this.reader = reader
    }

    fun untrack() {
        reader = null
    }

    fun current(): EntryImmersivePlace? = reader?.invoke()
}

/** Options that open the episode or chapter at this place. */
fun EntryImmersivePlace?.openOptions(): EntryOpenOptions = when (this) {
    is EntryImmersivePlace.ImagePage -> EntryOpenOptions(pageIndex = pageIndex)
    is EntryImmersivePlace.Playback -> EntryOpenOptions(startPositionMs = positionMs)
    null -> EntryOpenOptions()
}
