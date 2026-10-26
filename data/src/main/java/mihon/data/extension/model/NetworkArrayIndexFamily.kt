package mihon.data.extension.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import mihon.data.extension.model.lnreader.NetworkLnReaderPlugin

/** The families whose index is a bare JSON array: Tachiyomi's and Aniyomi's legacy `index.min.json`, and LNReader's. */
internal enum class ArrayIndexFamily(val listingName: String) {
    LEGACY("a Tachiyomi or Aniyomi extension"),
    LNREADER("an LNReader plugin"),
}

/**
 * Which family's index [entries] is. Each family trusts its extensions its own way, so this is never guessed: every
 * entry must read with that family's model and none with the other's. An empty index, which would fit both, and one
 * with an entry that doesn't fit, fail with what didn't.
 */
internal fun Json.arrayIndexFamily(entries: JsonArray): ArrayIndexFamily {
    require(entries.isNotEmpty()) { "This store lists no extensions" }
    val readings = entries.map { entry ->
        mapOf(
            ArrayIndexFamily.LEGACY to runCatching { decodeFromJsonElement<NetworkLegacyExtension>(entry) },
            ArrayIndexFamily.LNREADER to runCatching { decodeFromJsonElement<NetworkLnReaderPlugin>(entry) },
        )
    }
    // The family most entries fit is what the index means to be, so that's what a misfit is reported against
    val family = ArrayIndexFamily.entries.maxBy { family -> readings.count { it.getValue(family).isSuccess } }
    readings.forEachIndexed { index, reading ->
        reading.getValue(family).exceptionOrNull()?.let { cause ->
            throw IllegalArgumentException("Entry ${index + 1} isn't ${family.listingName}: ${cause.message}", cause)
        }
        val alsoFits = reading.filterKeys { it != family }.filterValues { it.isSuccess }.keys
        require(alsoFits.isEmpty()) { "Entry ${index + 1} reads as ${alsoFits.first().listingName} too" }
    }
    return family
}
