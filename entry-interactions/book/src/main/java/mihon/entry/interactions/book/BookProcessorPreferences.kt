package mihon.entry.interactions.book

import mihon.book.api.BookContentDescriptor
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import java.security.MessageDigest

/** Profile-scoped remembered processor choices keyed by the complete compatibility descriptor. */
internal class BookProcessorPreferences(
    private val preferenceStore: PreferenceStore,
) {
    private val choices = mutableMapOf<String, Preference<String>>()

    fun rememberedProcessorId(descriptor: BookContentDescriptor): String? {
        return descriptor.choice().get().ifBlank { null }
    }

    fun remember(descriptor: BookContentDescriptor, processorId: String) {
        require(processorId.isNotBlank()) { "remembered BOOK processor id must not be blank" }
        descriptor.choice().set(processorId)
    }

    fun forget(descriptor: BookContentDescriptor) {
        descriptor.choice().delete()
    }

    private fun BookContentDescriptor.choice(): Preference<String> = synchronized(choices) {
        choices.getOrPut(preferenceKey()) { preferenceStore.getString(preferenceKey()) }
    }

    private fun BookContentDescriptor.preferenceKey(): String {
        val identity = listOf(format, profile.orEmpty(), protection).joinToString(separator = "\u0000")
        val digest = MessageDigest.getInstance("SHA-256").digest(identity.encodeToByteArray())
        return "book_processor_${digest.joinToString("") { byte -> "%02x".format(byte) }}"
    }
}
