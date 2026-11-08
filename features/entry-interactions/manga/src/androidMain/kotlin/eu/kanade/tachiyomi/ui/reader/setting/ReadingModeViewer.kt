package eu.kanade.tachiyomi.ui.reader.setting

import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.openReaderPageInWebView
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.AndroidPagedViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagedViewer
import eu.kanade.tachiyomi.ui.reader.viewer.strip.AndroidStripViewer
import eu.kanade.tachiyomi.ui.reader.viewer.strip.StripViewer
import eu.kanade.tachiyomi.ui.reader.viewer.viewer
import mihon.entry.interactions.reader.settings.ReadingMode

/** The shared viewer for the reading mode [preference] resolves to, in [activity]'s view tree. */
internal fun ReadingMode.Companion.toViewer(preference: Int?, activity: ReaderActivity): Viewer {
    val shared = fromPreference(
        preference,
    ).viewer(activity.controller, openPageInWebView = activity::openReaderPageInWebView)
    return when (shared) {
        is PagedViewer -> AndroidPagedViewer(activity, shared)
        is StripViewer -> AndroidStripViewer(activity, shared)
        else -> error("No Android viewer for ${shared::class.simpleName}")
    }
}
