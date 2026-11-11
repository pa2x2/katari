package app.katari.desktop.display.scroll.natural

/**
 * Which way a touchpad swipe scrolls the app's content. The display reports the scrolling with the system's own
 * direction applied, so the app [turnsContent] around where it scrolls the other way; [contentFollowsFingers] where
 * it scrolls naturally, by which what follows the fingers rather than the content, as a volume swipe, knows which way
 * they went.
 */
internal data class TouchpadScrollDirection(val turnsContent: Boolean, val contentFollowsFingers: Boolean) {
    companion object {
        /**
         * The direction for [choice] on a system that scrolls naturally where [systemNatural], or that can't be read
         * where it is null. Then nothing tells whether the display's steps were turned, so they are taken to be as
         * [choice] says: a choice there tells the app what the system does rather than turning content around.
         */
        fun of(systemNatural: Boolean?, choice: NaturalScrollingChoice): TouchpadScrollDirection {
            val natural = when (choice) {
                // libinput's own default
                NaturalScrollingChoice.AsSystem -> systemNatural ?: false
                NaturalScrollingChoice.On -> true
                NaturalScrollingChoice.Off -> false
            }
            val system = systemNatural ?: natural
            return TouchpadScrollDirection(turnsContent = system != natural, contentFollowsFingers = natural)
        }
    }
}
