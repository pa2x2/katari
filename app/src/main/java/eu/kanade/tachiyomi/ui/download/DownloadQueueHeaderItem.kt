package eu.kanade.tachiyomi.ui.download

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractExpandableHeaderItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R

data class DownloadQueueHeaderItem(
    val model: DownloadQueueHeaderModel,
) : AbstractExpandableHeaderItem<DownloadQueueHeaderHolder, DownloadQueueItem>() {

    override fun getLayoutRes(): Int {
        return R.layout.download_header
    }

    override fun createViewHolder(
        view: View,
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
    ): DownloadQueueHeaderHolder {
        return DownloadQueueHeaderHolder(view, adapter)
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: DownloadQueueHeaderHolder,
        position: Int,
        payloads: List<Any?>?,
    ) {
        holder.bind(model)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DownloadQueueHeaderItem

        if (model != other.model) return false
        if (subItemsCount != other.subItemsCount) return false
        if (subItems !== other.subItems) return false

        return true
    }

    override fun hashCode(): Int {
        var result = model.hashCode()
        result = 31 * result + subItems.hashCode()
        return result
    }

    init {
        isHidden = false
        isExpanded = true
        isSelectable = false
    }
}
