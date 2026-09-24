package mihon.entry.viewer.settings

/**
 * Applies [value] as this entry's override, or clears the override when the value matches the profile default so an
 * unchanged selection never pins a redundant per-entry override.
 */
suspend fun <T> ViewerSettingBinding<T>.updateEntry(value: T) {
    val current = state.value
    if (value == (current.profileValue ?: current.processorDefault)) {
        clearEntryOverride()
    } else {
        setEntryOverride(value)
    }
}
