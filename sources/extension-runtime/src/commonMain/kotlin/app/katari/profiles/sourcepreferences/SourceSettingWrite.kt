package app.katari.profiles.sourcepreferences

/**
 * A write to [profileId]'s settings of the source with its own ID [sourceId]: of [key], or of every key when [key] is
 * null, as when the file was cleared.
 */
data class SourceSettingWrite(val profileId: Long, val sourceId: Long, val key: String?)
