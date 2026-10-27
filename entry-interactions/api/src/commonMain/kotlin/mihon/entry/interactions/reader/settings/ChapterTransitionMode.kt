package mihon.entry.interactions.reader.settings

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.*

/**
 * How the reader renders the slot between chapters.
 */
enum class ChapterTransitionMode(
    val titleRes: StringResource,
) {
    /**
     * Chapter card between every chapter, matching the historical default.
     */
    ALWAYS(MR.strings.chapter_transition_always),

    /**
     * Chapter card only when the neighbor chapter is unloaded or a chapter gap exists.
     */
    WHEN_NEEDED(MR.strings.chapter_transition_when_needed),

    /**
     * Compact loading indicator between contiguous chapters; the card remains for chapter gaps,
     * end of content and load failures because only loading can resolve those states.
     */
    HIDDEN(MR.strings.chapter_transition_hidden),
}
