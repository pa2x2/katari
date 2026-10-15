package eu.kanade.tachiyomi.ui.reader

import android.content.Context
import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.toArgb
import mihon.presentation.kit.theme.PageBackgroundColors

/**
 * The colour behind pages for the reader theme setting: 0 is white, 1 black, 2 dark grey, and 3 follows the night
 * mode with white or dark grey.
 */
@ColorInt
internal fun Context.readerPageBackgroundColor(readerTheme: Int): Int = when (readerTheme) {
    0 -> PageBackgroundColors.white
    2 -> PageBackgroundColors.gray
    3 -> if (isNightMode()) PageBackgroundColors.gray else PageBackgroundColors.white
    else -> PageBackgroundColors.black
}.toArgb()
