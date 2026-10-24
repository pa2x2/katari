package mihon.data.extension.model

import kotlinx.serialization.SerializationException

/**
 * Reads a v2 index or extension list in whichever of Katari's and Aniyomi's layouts makes sense of it, since neither
 * says which it is. Read in the other's layout, protobuf fails or finds extensions without sources: Katari's layout
 * fails on Aniyomi's torrent flag and misses its sources, Aniyomi's fails on Katari's sources. JSON uses the same names
 * in both, so there the extension library's form decides. An index that only one reading fits is read that way; one
 * that fits neither, or both, fails to load rather than being guessed at. With no extensions to judge by, either
 * reading lists the same nothing, and Katari's is used.
 */
internal fun <T : Any> readInFittingLayout(
    katari: () -> T,
    aniyomi: () -> T,
    extensionList: (T) -> NetworkExtensionList?,
): T {
    val katariReading = readOrNull(katari)
    val aniyomiReading = readOrNull(aniyomi)
    val katariList = katariReading?.let(extensionList)
    if (katariReading != null && (katariList == null || katariList.isEmpty)) return katariReading

    val fitting = listOfNotNull(
        katariReading?.takeIf { katariList?.fitsLayout() == true },
        aniyomiReading?.takeIf { extensionList(it)?.fitsLayout() != false },
    )
    return when (fitting.size) {
        1 -> fitting.single()
        0 -> throw IllegalArgumentException("The index fits neither Katari's nor Aniyomi's layout")
        else -> throw IllegalArgumentException("The index fits both Katari's and Aniyomi's layout")
    }
}

private fun <T : Any> readOrNull(read: () -> T): T? {
    return try {
        read()
    } catch (_: SerializationException) {
        null
    }
}
