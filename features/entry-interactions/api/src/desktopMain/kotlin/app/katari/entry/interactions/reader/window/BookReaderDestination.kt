package app.katari.entry.interactions.reader.window

import app.katari.core.common.platform.PlatformFacility
import app.katari.core.platform.PlatformContext

/** Where the desktop app shows the book reader: a screen of the window [open] is called from, which it fills. */
@PlatformFacility
interface BookReaderDestination {
    fun open(context: PlatformContext, request: BookReaderWindowRequest)
}

/** The chapter the book reader opens: [chapterId] of [entryId], where reading left off. */
data class BookReaderWindowRequest(
    val entryId: Long,
    val chapterId: Long,
)
