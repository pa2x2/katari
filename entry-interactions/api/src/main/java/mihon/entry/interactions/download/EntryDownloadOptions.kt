package mihon.entry.interactions.download

data class EntryDownloadOption(
    val key: String,
    val label: String,
)

data class EntryDownloadOptionGroup(
    val key: String,
    val label: String,
    val options: List<EntryDownloadOption>,
    val selectedKey: String? = null,
    /** The option a null [selectedKey] stands for, when nothing picked means an option chosen automatically. */
    val automaticKey: String? = null,
    val defaultLabel: String? = null,
    val required: Boolean = false,
    /** Picking another option lists the groups after this one again, with what the picked option offers. */
    val reloadsOptions: Boolean = false,
)

data class EntryDownloadOptions(
    val groups: List<EntryDownloadOptionGroup>,
    /** The source couldn't list the episode's options, so only the groups that don't come from it are offered. */
    val listingFailed: Boolean = false,
)

data class EntryDownloadOptionSelection(
    val values: Map<String, String?>,
)
