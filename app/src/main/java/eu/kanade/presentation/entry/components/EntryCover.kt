package eu.kanade.presentation.entry.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import coil3.compose.AsyncImage
import eu.kanade.presentation.util.rememberResourceBitmapPainter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import mihon.presentation.kit.content.CoverBacking
import mihon.presentation.kit.content.CoverFrame
import mihon.presentation.kit.content.coverFrame
import mihon.presentation.kit.content.rememberArtworkPlaceholder

enum class EntryCover(val ratio: Float) {
    Square(1f / 1f),
    Book(2f / 3f),
    LibraryWide(4f / 3f),
    Wide(16f / 9f),
    ;

    /**
     * The cover in [frame]. A cover fit inside its frame ([ContentScale.Fit]) is letterboxed; a [raised] one casts a
     * shadow, such as the cover at the top of an entry.
     */
    @Composable
    operator fun invoke(
        data: Any?,
        modifier: Modifier = Modifier,
        contentDescription: String = "",
        frame: CoverFrame = CoverFrame.Cover,
        contentScale: ContentScale = ContentScale.Crop,
        raised: Boolean = false,
        onClick: (() -> Unit)? = null,
    ) {
        AsyncImage(
            model = data,
            placeholder = rememberArtworkPlaceholder(),
            error = rememberResourceBitmapPainter(id = R.drawable.cover_error),
            contentDescription = contentDescription,
            modifier = modifier
                .aspectRatio(ratio)
                .coverFrame(
                    frame = frame,
                    backing = if (contentScale == ContentScale.Fit) CoverBacking.Letterbox else CoverBacking.None,
                    raised = raised,
                )
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            role = Role.Button,
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    },
                ),
            contentScale = contentScale,
        )
    }
}

fun EntryItemOrientation.toGridCoverType(): EntryCover {
    return when (this) {
        EntryItemOrientation.VERTICAL -> EntryCover.Book
        EntryItemOrientation.HORIZONTAL -> EntryCover.Wide
    }
}

fun EntryItemOrientation.toLibraryGridCoverType(): EntryCover {
    return when (this) {
        EntryItemOrientation.VERTICAL -> EntryCover.Book
        EntryItemOrientation.HORIZONTAL -> EntryCover.LibraryWide
    }
}

fun EntryItemOrientation.toListCoverType(): EntryCover {
    return when (this) {
        EntryItemOrientation.VERTICAL -> EntryCover.Square
        EntryItemOrientation.HORIZONTAL -> EntryCover.Wide
    }
}
