package mihon.data.extension.model

import kotlinx.serialization.SerializationException

/**
 * Reads a v2 index or extension list in whichever of Katari's and Aniyomi's layouts makes sense of it, since neither
 * says which it is. Read in the other's layout, protobuf fails or finds extensions without sources: Katari's layout
 * fails on Aniyomi's torrent flag and misses its sources, Aniyomi's fails on Katari's sources. JSON uses the same names
 * in both, so there the extension library's form decides.
 *
 * The reading in which more extensions fit is kept, with every extension it read: one odd listing, such as an
 * extension published without sources, mustn't take the whole store down. An index that fits neither reading, or both
 * equally, fails to load rather than being guessed at. With no extensions to judge by, either reading lists the same
 * nothing, and Katari's is used.
 */
internal fun <T : Any> readInFittingLayout(
    katari: () -> T,
    aniyomi: () -> T,
    extensionList: (T) -> NetworkExtensionList?,
): T {
    val katariReading = readOrNull(katari)
    val aniyomiReading = readOrNull(aniyomi)
    listOfNotNull(katariReading, aniyomiReading)
        .firstOrNull { extensionList(it)?.isEmpty != false }
        ?.let { return it }

    val katariFitting = katariReading?.let(extensionList)?.countFitting() ?: 0
    val aniyomiFitting = aniyomiReading?.let(extensionList)?.countFitting() ?: 0
    return when {
        katariFitting > aniyomiFitting -> katariReading!!
        aniyomiFitting > katariFitting -> aniyomiReading!!
        katariFitting == 0 -> throw IllegalArgumentException("The index fits neither Katari's nor Aniyomi's layout")
        else -> throw IllegalArgumentException("The index fits Katari's and Aniyomi's layout equally")
    }
}

private fun <T : Any> readOrNull(read: () -> T): T? {
    return try {
        read()
    } catch (_: SerializationException) {
        null
    }
}
