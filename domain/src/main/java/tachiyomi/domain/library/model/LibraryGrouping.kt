package tachiyomi.domain.library.model

data class LibraryGrouping(
    val dimensions: List<LibraryGroupingDimension>,
) {
    init {
        require(dimensions.size <= LibraryGroupingDimension.entries.size)
        require(dimensions.distinct().size == dimensions.size)
    }

    object Serializer {
        private const val VERSION_PREFIX = "v2:"
        private const val NONE = "none"

        fun serialize(grouping: LibraryGrouping): String {
            val value = grouping.dimensions
                .joinToString(separator = ",", transform = LibraryGroupingDimension::preferenceValue)
                .ifEmpty { NONE }
            return VERSION_PREFIX + value
        }

        fun deserialize(value: String): LibraryGrouping {
            legacyValues[value]?.let { return it }
            if (!value.startsWith(VERSION_PREFIX)) return default

            val serializedDimensions = value.removePrefix(VERSION_PREFIX)
            if (serializedDimensions == NONE) return LibraryGrouping(emptyList())

            val dimensions = serializedDimensions
                .split(',')
                .mapNotNull(LibraryGroupingDimension::fromPreferenceValue)
                .distinct()
            return dimensions.takeIf { it.isNotEmpty() }
                ?.let(::LibraryGrouping)
                ?: default
        }
    }

    companion object {
        val default = LibraryGrouping(listOf(LibraryGroupingDimension.Category))

        private val legacyValues = mapOf(
            "Category" to LibraryGrouping(listOf(LibraryGroupingDimension.Category)),
            "Type" to LibraryGrouping(listOf(LibraryGroupingDimension.EntryType)),
            "Extension" to LibraryGrouping(listOf(LibraryGroupingDimension.Source)),
            "TypeCategory" to LibraryGrouping(
                listOf(LibraryGroupingDimension.EntryType, LibraryGroupingDimension.Category),
            ),
            "CategoryType" to LibraryGrouping(
                listOf(LibraryGroupingDimension.Category, LibraryGroupingDimension.EntryType),
            ),
            "ExtensionCategory" to LibraryGrouping(
                listOf(LibraryGroupingDimension.Source, LibraryGroupingDimension.Category),
            ),
            "CategoryExtension" to LibraryGrouping(
                listOf(LibraryGroupingDimension.Category, LibraryGroupingDimension.Source),
            ),
        )
    }
}

enum class LibraryGroupingDimension(
    val preferenceValue: String,
) {
    Category("category"),
    EntryType("entry_type"),
    Source("source"),
    ;

    companion object {
        fun fromPreferenceValue(value: String): LibraryGroupingDimension? {
            return entries.firstOrNull { it.preferenceValue == value }
        }
    }
}
