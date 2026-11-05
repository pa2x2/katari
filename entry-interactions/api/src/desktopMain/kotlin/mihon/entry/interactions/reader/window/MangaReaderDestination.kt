package mihon.entry.interactions.reader.window

import mihon.core.common.platform.PlatformFacility
import mihon.core.platform.PlatformContext

/** Where the desktop app shows the manga reader: a screen of the window [open] is called from, which it fills. */
@PlatformFacility
interface MangaReaderDestination {
    fun open(context: PlatformContext, request: MangaReaderRequest)
}

/** The chapter a reader opens: [chapterId] of [entryId], at [pageIndex] when given and else where reading left off. */
data class MangaReaderRequest(
    val entryId: Long,
    val chapterId: Long,
    val pageIndex: Int?,
)
