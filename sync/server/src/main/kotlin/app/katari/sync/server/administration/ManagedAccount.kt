package app.katari.sync.server.administration

import app.katari.sync.server.account.AccountSummary
import app.katari.sync.server.device.Device

/** One account as an admin manages it. */
data class ManagedAccount(
    val summary: AccountSummary,
    /** Then it can't be demoted, disabled or deleted. */
    val isLastEnabledAdmin: Boolean,
    /** Linked and not revoked, oldest first. */
    val devices: List<Device>,
)
