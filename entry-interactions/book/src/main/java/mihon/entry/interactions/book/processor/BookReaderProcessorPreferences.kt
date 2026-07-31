package mihon.entry.interactions.book.processor

import mihon.book.api.model.BookPublicationModelDescriptor
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.ProfilePreferenceKeyPattern
import java.security.MessageDigest

/** Profile-scoped remembered reader choices keyed by prepared publication-model family. */
internal class BookReaderProcessorPreferences(
    private val preferenceStore: PreferenceStore,
) {
    companion object {
        val KEY_FAMILY = ProfilePreferenceKeyPattern.Prefix("book_processor_")
        val profileKeyPatterns = setOf(KEY_FAMILY)
    }

    private val choices = mutableMapOf<String, Preference<String>>()

    fun rememberedProcessorId(model: BookPublicationModelDescriptor): String? {
        return model.choice().get().ifBlank { null }
    }

    fun remember(model: BookPublicationModelDescriptor, processorId: String) {
        require(processorId.isNotBlank()) { "remembered BOOK processor id must not be blank" }
        model.choice().set(processorId)
    }

    fun forget(model: BookPublicationModelDescriptor) {
        model.choice().delete()
    }

    private fun BookPublicationModelDescriptor.choice(): Preference<String> = synchronized(choices) {
        choices.getOrPut(preferenceKey()) { preferenceStore.getString(preferenceKey()) }
    }

    private fun BookPublicationModelDescriptor.preferenceKey(): String {
        val identity = "$id\u0000$version"
        val digest = MessageDigest.getInstance("SHA-256").digest(identity.encodeToByteArray())
        return KEY_FAMILY.key(digest.joinToString("") { byte -> "%02x".format(byte) })
    }
}
