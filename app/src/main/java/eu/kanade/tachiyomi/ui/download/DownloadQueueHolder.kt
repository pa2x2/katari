package eu.kanade.tachiyomi.ui.download

import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.DownloadItemBinding
import eu.kanade.tachiyomi.util.view.popupMenu

class DownloadQueueHolder(private val view: View, val adapter: DownloadQueueAdapter) :
    FlexibleViewHolder(view, adapter) {

    private val binding = DownloadItemBinding.bind(view)

    init {
        setDragHandleView(binding.reorder)
        binding.menu.setOnClickListener { it.post { showPopupMenu(it) } }
    }

    private lateinit var item: DownloadQueueItem

    fun bind(item: DownloadQueueItem) {
        this.item = item
        bindModel(item.model())
    }

    fun notifyProgress() {
        val model = item.model()
        if (binding.downloadProgress.max != model.progressMax) {
            binding.downloadProgress.max = model.progressMax
        }
        binding.downloadProgress.setProgressCompat(model.progress, true)
    }

    fun notifyProgressText() {
        binding.downloadProgressText.text = item.model().progressText
    }

    override fun onItemReleased(position: Int) {
        super.onItemReleased(position)
        adapter.downloadItemListener.onItemReleased(position)
        binding.container.isDragged = false
    }

    override fun onActionStateChanged(position: Int, actionState: Int) {
        super.onActionStateChanged(position, actionState)
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            binding.container.isDragged = true
        }
    }

    private fun bindModel(model: DownloadQueueItemModel) {
        binding.chapterTitle.text = model.subtitle
        binding.mangaFullTitle.text = model.title
        binding.downloadProgress.max = model.progressMax
        binding.downloadProgress.progress = model.progress
        binding.downloadProgressText.text = model.progressText
    }

    private fun showPopupMenu(view: View) {
        view.popupMenu(
            menuRes = R.menu.download_single,
            initMenu = {
                findItem(R.id.move_to_top).isVisible = bindingAdapterPosition > 1
                findItem(R.id.move_to_bottom).isVisible =
                    bindingAdapterPosition != adapter.itemCount - 1
            },
            onMenuItemClick = {
                adapter.downloadItemListener.onMenuItemClick(bindingAdapterPosition, this)
            },
        )
    }
}
