package eu.kanade.tachiyomi.ui.download

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractSectionableItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R

class DownloadQueueItem(
    val payload: Any,
    header: DownloadQueueHeaderItem,
    private val modelProvider: () -> DownloadQueueItemModel,
) : AbstractSectionableItem<DownloadQueueHolder, DownloadQueueHeaderItem>(header) {

    fun model(): DownloadQueueItemModel = modelProvider()

    inline fun <reified T> payloadAs(): T? = payload as? T

    override fun getLayoutRes(): Int {
        return R.layout.download_item
    }

    override fun createViewHolder(
        view: View,
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
    ): DownloadQueueHolder {
        return DownloadQueueHolder(view, adapter as DownloadQueueAdapter)
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: DownloadQueueHolder,
        position: Int,
        payloads: MutableList<Any>,
    ) {
        holder.bind(this)
    }

    override fun isDraggable(): Boolean {
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is DownloadQueueItem) {
            val model = model()
            val otherModel = other.model()
            return model.id == otherModel.id && model.contentType == otherModel.contentType
        }
        return false
    }

    override fun hashCode(): Int {
        val model = model()
        return 31 * model.id.hashCode() + model.contentType.hashCode()
    }
}
