package app.katari.presentation.kit.content

/**
 * [name] with a line break allowed after each `.`, `-` and `_`, for a file's name shown as text. A name has no spaces,
 * so where it doesn't fit on one line it would otherwise break wherever the line ends, such as "0.18.2.1.tachi" / "bk".
 */
fun breakableFileName(name: String): String = buildString(name.length) {
    name.forEach { char ->
        append(char)
        if (char in FILE_NAME_BREAKS) append(ZERO_WIDTH_SPACE)
    }
}

private const val FILE_NAME_BREAKS = ".-_"
private const val ZERO_WIDTH_SPACE = '​'
