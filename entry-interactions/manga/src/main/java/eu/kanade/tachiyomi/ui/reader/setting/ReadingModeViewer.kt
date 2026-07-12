package eu.kanade.tachiyomi.ui.reader.setting

import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.L2RPagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.R2LPagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.VerticalPagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import mihon.entry.interactions.reader.settings.ReadingMode

internal fun ReadingMode.Companion.toViewer(preference: Int?, activity: ReaderActivity): Viewer {
    return when (fromPreference(preference)) {
        ReadingMode.LEFT_TO_RIGHT -> L2RPagerViewer(activity)
        ReadingMode.RIGHT_TO_LEFT -> R2LPagerViewer(activity)
        ReadingMode.VERTICAL -> VerticalPagerViewer(activity)
        ReadingMode.WEBTOON -> WebtoonViewer(activity)
        ReadingMode.CONTINUOUS_VERTICAL -> WebtoonViewer(activity, isContinuous = false)
        ReadingMode.DEFAULT -> throw IllegalStateException("Preference value must be resolved: $preference")
    }
}
