package mihon.entry.interactions

import dev.icerock.moko.resources.StringResource

@JvmInline
value class EntryTrackingServiceId(val value: Long)

data class EntryTrackingServiceDescriptor(
    val id: EntryTrackingServiceId,
    val name: String,
    val logoResource: Int,
    val capabilities: EntryTrackingServiceCapabilities,
)

data class EntryTrackingServiceCapabilities(
    val statuses: List<EntryTrackingStatus>,
    val scores: List<String>,
    val supportsReadingDates: Boolean,
    val supportsPrivateTracking: Boolean,
    val supportsRemoteDeletion: Boolean,
    val supportsAutomaticBinding: Boolean,
)

data class EntryTrackingStatus(
    val value: Long,
    val label: StringResource?,
)
