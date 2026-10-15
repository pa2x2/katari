package mihon.entry.interactions.runtime

import mihon.entry.viewer.settings.shared.ReaderCapabilityId

/**
 * The reader capabilities an Entry type may offer on one viewer settings surface. Shared reader settings use them to
 * decide which surfaces a setting can apply to before any reader is open.
 */
data class ReaderSettingsSurfaceCapabilities(
    val surfaceId: String,
    val capabilities: Set<ReaderCapabilityId>,
) {
    init {
        require(surfaceId.isNotBlank()) { "Reader settings surface ID must not be blank" }
    }
}
