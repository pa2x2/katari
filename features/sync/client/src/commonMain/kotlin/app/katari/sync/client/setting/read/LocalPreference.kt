package app.katari.sync.client.setting.read

/** A preference as this device stores it: [key] in profile [profileId]'s store, or the shared one when null. */
internal data class LocalPreference(val profileId: Long?, val key: String)
