package app.katari.core.platform.clipboard

import app.katari.core.common.platform.PlatformFacility

/** The system clipboard, for text the user copies. */
@PlatformFacility
interface TextClipboard {
    /**
     * Puts [text] on the clipboard under [label], which platforms that show clipboard contents use to describe it, and
     * confirms the copy where the platform itself doesn't. Blank text is ignored. Never throws: the implementation
     * deals with a clipboard it can't write to.
     */
    fun copy(label: String, text: String)
}
