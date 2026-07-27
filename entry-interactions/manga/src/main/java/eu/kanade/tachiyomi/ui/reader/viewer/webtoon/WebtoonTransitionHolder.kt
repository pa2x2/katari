package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderTransitionView
import eu.kanade.tachiyomi.util.system.dpToPx
import mihon.entry.interactions.viewer.EntryChildTransition

/**
 * Holder of the webtoon viewer that contains a chapter transition.
 */
internal class WebtoonTransitionHolder(
    val layout: LinearLayout,
    viewer: WebtoonViewer,
) : WebtoonBaseHolder(layout, viewer) {

    private val transitionView = ReaderTransitionView(context)

    init {
        layout.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER

        val paddingVertical = 128.dpToPx
        val paddingHorizontal = 32.dpToPx
        layout.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)

        layout.addView(transitionView)
    }

    /**
     * Binds the given [transition] with this view holder, subscribing to its state.
     */
    fun bind(transition: EntryChildTransition<ReaderChapter>) {
        transitionView.bind(
            transition = transition,
            downloadManager = viewer.downloadManager,
            onRetry = viewer.activity::requestTransitionChapterLoad,
        )
    }

    /**
     * Called when the view is recycled and being added to the view pool.
     */
    override fun recycle() {
        // Compose state collection is detached with the recycled transition view.
    }
}
