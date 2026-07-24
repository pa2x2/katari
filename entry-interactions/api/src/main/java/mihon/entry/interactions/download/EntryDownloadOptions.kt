package mihon.entry.interactions

data class EntryDownloadOption(
    val key: String,
    val label: String,
)

data class EntryDownloadOptionGroup(
    val key: String,
    val label: String,
    val options: List<EntryDownloadOption>,
    val selectedKey: String? = null,
    val defaultLabel: String? = null,
    val required: Boolean = false,
)

data class EntryDownloadOptions(
    val groups: List<EntryDownloadOptionGroup>,
)

data class EntryDownloadOptionSelection(
    val values: Map<String, String?>,
)
