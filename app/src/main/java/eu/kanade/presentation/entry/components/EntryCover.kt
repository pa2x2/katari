package eu.kanade.presentation.entry.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import coil3.compose.AsyncImage
import eu.kanade.presentation.util.rememberResourceBitmapPainter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation

enum class EntryCover(val ratio: Float) {
    Square(1f / 1f),
    Book(2f / 3f),
    LibraryWide(4f / 3f),
    Wide(16f / 9f),
    ;

    @Composable
    operator fun invoke(
        data: Any?,
        modifier: Modifier = Modifier,
        contentDescription: String = "",
        shape: Shape = MaterialTheme.shapes.extraSmall,
        contentScale: ContentScale = ContentScale.Crop,
        backgroundColor: Color = Color.Transparent,
        onClick: (() -> Unit)? = null,
    ) {
        AsyncImage(
            model = data,
            placeholder = ColorPainter(CoverPlaceholderColor),
            error = rememberResourceBitmapPainter(id = R.drawable.cover_error),
            contentDescription = contentDescription,
            modifier = modifier
                .aspectRatio(ratio)
                .clip(shape)
                .background(backgroundColor)
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

private val CoverPlaceholderColor = Color(0x1F888888)
