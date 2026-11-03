package eu.kanade.tachiyomi.data.coil

import coil3.Bitmap

/** An icon's pixels row by row, as ARGB ints. */
internal class IconPixels(val width: Int, val height: Int, val argb: IntArray)

internal expect fun Bitmap.iconPixels(): IconPixels

/** A new bitmap of [square]'s part of this one. */
internal expect fun Bitmap.croppedTo(square: IconSquare): Bitmap
