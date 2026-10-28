package eu.kanade.presentation.browse.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.zacsweers.metro.Inject
import tachiyomi.domain.source.model.Source

/** Browse's own icon and row, with the icons and stores of the extensions Android loads. */
@Inject
class AndroidSourceRows : SourceRows {
    @Composable
    override fun Icon(source: Source, modifier: Modifier) {
        SourceIcon(source = source, modifier = modifier)
    }

    @Composable
    override fun Row(source: Source, onClick: () -> Unit) {
        BaseSourceItem(source = source, onClickItem = onClick)
    }
}
