package mihon.entry.interactions.book.document.reader

import androidx.compose.foundation.text.contextmenu.data.TextContextMenuComponent
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuData
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSeparator
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider

/** Keys of the menu items the reader contributes to its text selection menu. */
internal sealed interface BookDocumentSelectionMenuKey {
    data object Copy : BookDocumentSelectionMenuKey
    data object Listen : BookDocumentSelectionMenuKey
    data object Translate : BookDocumentSelectionMenuKey
}

/**
 * Compose collects text menu components from the selection node outwards, so items appended on
 * the selection container always follow the platform's Select all and PROCESS_TEXT activities.
 * The floating toolbar fills its row in that order, which left the reader's Copy and actions in
 * the overflow. Moving them to the front is only possible once the menu data is assembled.
 */
internal fun TextContextMenuDataProvider.withReaderItemsFirst(): TextContextMenuDataProvider {
    val platform = this
    return object : TextContextMenuDataProvider by platform {
        override fun data(): TextContextMenuData = platform.data().withReaderItemsFirst()
    }
}

private fun TextContextMenuData.withReaderItemsFirst(): TextContextMenuData {
    val (reader, platform) = components.partition { it.key is BookDocumentSelectionMenuKey }
    if (reader.isEmpty()) return this
    val ordered = reader + TextContextMenuSeparator + platform
    return TextContextMenuData(
        ordered.filterIndexed { index, component ->
            !component.isSeparator() ||
                (index > 0 && index < ordered.lastIndex && !ordered[index + 1].isSeparator())
        },
    )
}

private fun TextContextMenuComponent.isSeparator() = this === TextContextMenuSeparator
