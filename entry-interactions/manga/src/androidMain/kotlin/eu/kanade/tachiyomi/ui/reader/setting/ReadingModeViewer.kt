package eu.kanade.tachiyomi.ui.reader.setting

import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.AndroidPagedViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerDirection
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import mihon.entry.interactions.reader.settings.ReadingMode

internal fun ReadingMode.Companion.toViewer(preference: Int?, activity: ReaderActivity): Viewer {
    return when (fromPreference(preference)) {
        ReadingMode.LEFT_TO_RIGHT -> AndroidPagedViewer(activity, PagerDirection.LeftToRight)
        ReadingMode.RIGHT_TO_LEFT -> AndroidPagedViewer(activity, PagerDirection.RightToLeft)
        ReadingMode.VERTICAL -> AndroidPagedViewer(activity, PagerDirection.Vertical)
        ReadingMode.WEBTOON -> WebtoonViewer(activity)
        ReadingMode.CONTINUOUS_VERTICAL -> WebtoonViewer(activity, isContinuous = false)
        ReadingMode.DEFAULT -> throw IllegalStateException("Preference value must be resolved: $preference")
    }
}
