package eu.kanade.tachiyomi.ui.download

import android.view.MenuItem
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem

class DownloadQueueAdapter(
    val downloadItemListener: DownloadQueueItemListener,
) : FlexibleAdapter<AbstractFlexibleItem<*>>(
    null,
    downloadItemListener,
    true,
) {

    override fun shouldMove(fromPosition: Int, toPosition: Int): Boolean {
        return getHeaderOf(getItem(fromPosition)) == getHeaderOf(getItem(toPosition))
    }

    interface DownloadQueueItemListener {
        fun onItemReleased(position: Int)
        fun onMenuItemClick(position: Int, menuItem: MenuItem)
    }
}
