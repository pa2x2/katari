package mihon.entry.interactions.host.tracking

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import mihon.entry.interactions.tracking.host.EntryTrackingHost

/**
 * The trackers as the host of entry interactions' tracking feature. Each application graph includes it; validation
 * graphs supply a controlled host instead, so it is not contributed.
 */
@BindingContainer
interface EntryTrackingHostBindings {
    @Binds
    val AppEntryTrackingHost.entryTrackingHost: EntryTrackingHost
}
