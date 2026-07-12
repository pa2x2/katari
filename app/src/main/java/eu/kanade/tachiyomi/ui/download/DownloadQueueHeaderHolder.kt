package eu.kanade.tachiyomi.ui.download

import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.viewholders.ExpandableViewHolder
import eu.kanade.tachiyomi.databinding.DownloadHeaderBinding

class DownloadQueueHeaderHolder(view: View, adapter: FlexibleAdapter<*>) : ExpandableViewHolder(view, adapter) {

    private val binding = DownloadHeaderBinding.bind(view)

    fun bind(model: DownloadQueueHeaderModel) {
        setDragHandleView(binding.reorder)
        binding.title.text = model.displayTitle
    }

    override fun onActionStateChanged(position: Int, actionState: Int) {
        super.onActionStateChanged(position, actionState)
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            binding.container.isDragged = true
            mAdapter.collapseAll()
        }
    }

    override fun onItemReleased(position: Int) {
        super.onItemReleased(position)
        binding.container.isDragged = false
        mAdapter.expandAll()
        (mAdapter as DownloadQueueAdapter).downloadItemListener.onItemReleased(position)
    }
}
