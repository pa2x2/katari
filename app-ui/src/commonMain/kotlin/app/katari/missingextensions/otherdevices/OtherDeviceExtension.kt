package app.katari.missingextensions.otherdevices

import app.katari.sync.client.extension.OtherDeviceInstall
import eu.kanade.tachiyomi.extension.model.Extension

/** An extension another device of the account installed that isn't here, or uninstalled that is. */
data class OtherDeviceExtension(
    val change: OtherDeviceInstall,
    val name: String,
    /** For an install, the listing that installs it here. */
    val listing: Extension.Available?,
    /** For an uninstall, the extension as installed here. */
    val installed: Extension.Installed?,
    /** For an uninstall, how many library entries each profile has from it, by profile ID. */
    val countsByProfile: Map<Long, Long>,
) {
    val pkgName: String
        get() = change.pkgName

    /** Tells this change of the extension apart from its earlier and later ones. */
    val changeKey: String
        get() = "${change.pkgName}:${if (change.installed) "installed" else "uninstalled"}:${change.changedAt}"

    val titleCount: Long
        get() = countsByProfile.values.sum()
}

/** What the account's other devices did that this device hasn't. */
data class OtherDeviceChanges(
    val installs: List<OtherDeviceExtension>,
    val uninstalls: List<OtherDeviceExtension>,
) {
    val all: List<OtherDeviceExtension>
        get() = installs + uninstalls

    companion object {
        val NONE = OtherDeviceChanges(emptyList(), emptyList())
    }
}
