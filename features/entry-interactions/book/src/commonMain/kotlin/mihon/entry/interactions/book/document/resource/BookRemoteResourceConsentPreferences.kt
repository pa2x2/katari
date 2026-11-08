package mihon.entry.interactions.book.document.resource

import tachiyomi.core.common.preference.PreferenceStore
import java.security.MessageDigest

/** Profile-owned publication-origin approvals. */
internal class BookRemoteResourceConsentPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun approvedOrigins(publicationId: String): Set<String> = preferenceStore
        .getStringSet(key(publicationId), emptySet())
        .get()

    fun approve(publicationId: String, origins: Set<String>) {
        val preference = preferenceStore.getStringSet(key(publicationId), emptySet())
        preference.set(preference.get() + origins)
    }

    private fun key(publicationId: String): String = KEY_PREFIX + MessageDigest.getInstance("SHA-256")
        .digest(publicationId.encodeToByteArray())
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    companion object {
        const val KEY_PREFIX = "entry_interactions_book_remote_resource_consent_"
    }
}
