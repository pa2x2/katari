package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderTransitionView
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.widget.ViewPagerAdapter
import mihon.entry.interactions.viewer.EntryChildTransition

/**
 * View of the ViewPager that contains a chapter transition.
 */
@SuppressLint("ViewConstructor")
internal class PagerTransitionHolder(
    readerThemedContext: Context,
    val viewer: PagerViewer,
    val transition: EntryChildTransition<ReaderChapter>,
) : LinearLayout(readerThemedContext), ViewPagerAdapter.PositionableView {

    /**
     * Item that identifies this view. Needed by the adapter to not recreate views.
     */
    override val item: Any
        get() = transition

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        val sidePadding = 64.dpToPx
        setPadding(sidePadding, 0, sidePadding, 0)

        val transitionView = ReaderTransitionView(context)
        addView(transitionView)
        transitionView.bind(
            transition = transition,
            downloadManager = viewer.downloadManager,
            onRetry = viewer.activity::requestTransitionChapterLoad,
        )
    }
}
