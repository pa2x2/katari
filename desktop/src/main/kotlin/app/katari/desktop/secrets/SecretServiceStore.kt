package app.katari.desktop.secrets

import app.katari.desktop.application.DesktopAppVariant
import app.katari.desktop.dbus.DbusConnection
import app.katari.desktop.dbus.DbusException
import app.katari.desktop.dbus.Variant
import app.katari.translation.provider.server.SecretStore
import app.katari.translation.provider.server.ServerConnectionId
import dev.icerock.moko.resources.StringResource
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import tachiyomi.i18n.server_connection_security_description_keyring
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes

/**
 * Linux: the desktop's keyring through the freedesktop Secret Service API, which GNOME Keyring and KWallet provide on
 * the session bus. A key is an item of the default collection, found by the attributes [ATTRIBUTE_APPLICATION], this
 * build's application ID, so that another build of the app never reads its keys, and [ATTRIBUTE_NAME]; a locked keyring
 * asks the user to unlock it in the keyring's own window. Keys are remembered once read or written, so translating
 * doesn't ask the keyring before every call; a read that fails is tried again next time.
 *
 * A key whose unlock window the user dismissed, or left unanswered, is read from then on only while the keyring is
 * unlocked, as by signing in to the desktop session, without asking again until [allowAsking] or the next launch.
 */
class SecretServiceStore : SecretStore {
    private val remembered = ConcurrentHashMap<String, Remembered>()

    // The aliases of keys whose unlock window wasn't answered with an unlock
    private val declined: MutableSet<String> = ConcurrentHashMap.newKeySet()

    @Volatile
    private var available = false

    // A keyring that isn't running yet, as when the app starts with the session, may be there on a later look
    override val isAvailable: Boolean
        get() = available || hasSecretService().also { available = it }

    private fun hasSecretService(): Boolean =
        DbusConnection.session()?.use { bus ->
            try {
                val owned = bus.call(BUS, BUS_PATH, BUS, "NameHasOwner", "s", SERVICE).first() == true
                owned || SERVICE in bus.call(BUS, BUS_PATH, BUS, "ListActivatableNames").first() as List<*>
            } catch (e: DbusException) {
                logcat(LogPriority.WARN, e) { "Can't ask the session bus for the Secret Service" }
                false
            } catch (e: IOException) {
                logcat(LogPriority.WARN, e) { "Can't ask the session bus for the Secret Service" }
                false
            }
        } ?: false

    override val description: StringResource = MR.strings.server_connection_security_description_keyring

    override fun read(connection: ServerConnectionId): String? {
        remembered[connection.apiKeyAlias]?.let { return it.value }
        if (!isAvailable) throw IOException("The desktop has no keyring running")
        val alias = connection.apiKeyAlias
        return try {
            session { readSecret(connection, mayAsk = alias !in declined) }.also { remembered[alias] = Remembered(it) }
        } catch (e: PromptNotAnswered) {
            declined += alias
            throw e
        } catch (e: DbusException) {
            throw IOException("The keyring didn't give $alias", e)
        }
    }

    override fun allowAsking(connection: ServerConnectionId) {
        declined -= connection.apiKeyAlias
    }

    override fun write(connection: ServerConnectionId, value: String) {
        if (!isAvailable) throw IOException("The desktop has no keyring")
        try {
            session {
                if (value.isBlank()) deleteSecret(connection) else storeSecret(connection, value)
            }
        } catch (e: DbusException) {
            throw IOException("The keyring didn't save ${connection.apiKeyAlias}", e)
        }
        remembered[connection.apiKeyAlias] = Remembered(value.takeUnless(String::isBlank))
        declined -= connection.apiKeyAlias
    }

    private fun <T> session(block: Session.() -> T): T {
        val bus = DbusConnection.session() ?: throw IOException("Can't reach the session bus")
        return bus.use {
            val (_, path) = bus.call(
                SERVICE,
                SERVICE_PATH,
                SERVICE_INTERFACE,
                "OpenSession",
                "sv",
                "plain",
                Variant("s", ""),
            )
            Session(bus, path as String).block()
        }
    }

    private class Session(private val bus: DbusConnection, private val path: String) {
        /** Throws when the key is in a locked part of the keyring and [mayAsk] is false. */
        fun readSecret(connection: ServerConnectionId, mayAsk: Boolean): String? {
            val (unlocked, locked) = search(connection)
            val item = unlocked.firstOrNull() ?: when {
                locked.isEmpty() -> return null
                !mayAsk -> throw IOException("The keyring is locked")
                else -> unlock(locked).firstOrNull() ?: throw IOException("The keyring stayed locked")
            }
            val (secret) = bus.call(SERVICE, item, ITEM_INTERFACE, "GetSecret", "o", path)
            val value = (secret as List<*>)[2] as ByteArray
            return String(value, Charsets.UTF_8)
        }

        fun storeSecret(connection: ServerConnectionId, value: String) {
            val collection = unlock(listOf(defaultCollection())).single()
            val properties = mapOf(
                "$ITEM_INTERFACE.Label" to Variant("s", connection.apiKeyLabel),
                "$ITEM_INTERFACE.Attributes" to Variant("a{ss}", attributes(connection)),
            )
            val secret = listOf(path, ByteArray(0), value.toByteArray(Charsets.UTF_8), "text/plain; charset=utf8")
            val (_, prompt) = bus.call(
                SERVICE,
                collection,
                COLLECTION_INTERFACE,
                "CreateItem",
                "a{sv}(oayays)b",
                properties,
                secret,
                true,
            )
            awaitPrompt(prompt as String)
        }

        fun deleteSecret(connection: ServerConnectionId) {
            unlockedItems(connection).forEach { item ->
                val (prompt) = bus.call(SERVICE, item, ITEM_INTERFACE, "Delete")
                awaitPrompt(prompt as String)
            }
        }

        private fun unlockedItems(connection: ServerConnectionId): List<String> {
            val (unlocked, locked) = search(connection)
            return unlocked + unlock(locked)
        }

        /** The items holding [connection]'s key, unlocked and locked, found without asking the user anything. */
        private fun search(connection: ServerConnectionId): Pair<List<String>, List<String>> {
            val (unlocked, locked) = bus.call(
                SERVICE,
                SERVICE_PATH,
                SERVICE_INTERFACE,
                "SearchItems",
                "a{ss}",
                attributes(connection),
            )
            return (unlocked as List<*>).map { it as String } to (locked as List<*>).map { it as String }
        }

        private fun unlock(objects: List<String>): List<String> {
            if (objects.isEmpty()) return emptyList()
            val (unlocked, prompt) = bus.call(SERVICE, SERVICE_PATH, SERVICE_INTERFACE, "Unlock", "ao", objects)
            val promptResult = awaitPrompt(prompt as String)
            return (unlocked as List<*>).map { it as String } +
                ((promptResult?.value as? List<*>)?.map { it as String } ?: emptyList())
        }

        // The keyring's default collection, created where the keyring has none yet
        private fun defaultCollection(): String {
            val (alias) = bus.call(SERVICE, SERVICE_PATH, SERVICE_INTERFACE, "ReadAlias", "s", DEFAULT_ALIAS)
            if (alias != NO_OBJECT) return alias as String
            val (collection, prompt) = bus.call(
                SERVICE,
                SERVICE_PATH,
                SERVICE_INTERFACE,
                "CreateCollection",
                "a{sv}s",
                mapOf("$COLLECTION_INTERFACE.Label" to Variant("s", DEFAULT_COLLECTION_LABEL)),
                DEFAULT_ALIAS,
            )
            if (collection != NO_OBJECT) return collection as String
            return awaitPrompt(prompt as String)?.value as? String ?: throw DbusException(DISMISSED, "No collection")
        }

        /**
         * Shows [prompt], the keyring's own window asking the user to unlock it or allow a change, and waits for the
         * answer, whose result is returned; none is needed where [prompt] is no object. Throws [PromptNotAnswered] when
         * it is dismissed or left unanswered.
         */
        private fun awaitPrompt(prompt: String): Variant? {
            if (prompt == NO_OBJECT) return null
            bus.call(
                DbusConnection.BUS_NAME,
                DbusConnection.BUS_PATH,
                DbusConnection.BUS_NAME,
                "AddMatch",
                "s",
                "type='signal',interface='$PROMPT_INTERFACE',member='Completed',path='$prompt'",
            )
            bus.call(SERVICE, prompt, PROMPT_INTERFACE, "Prompt", "s", "")
            val (dismissed, result) = bus.awaitSignal(prompt, PROMPT_INTERFACE, "Completed", PROMPT_TIMEOUT)
                ?: throw PromptNotAnswered("The keyring's window wasn't answered in time")
            if (dismissed == true) throw PromptNotAnswered("The keyring's window was dismissed")
            return result as Variant
        }

        private fun attributes(connection: ServerConnectionId) = mapOf(
            ATTRIBUTE_APPLICATION to DesktopAppVariant.current.applicationId,
            ATTRIBUTE_NAME to connection.apiKeyAlias,
        )
    }

    private class Remembered(val value: String?)

    private class PromptNotAnswered(message: String) : IOException(message)

    private companion object {
        const val BUS = DbusConnection.BUS_NAME
        const val BUS_PATH = DbusConnection.BUS_PATH
        const val SERVICE = "org.freedesktop.secrets"
        const val SERVICE_PATH = "/org/freedesktop/secrets"
        const val SERVICE_INTERFACE = "org.freedesktop.Secret.Service"
        const val COLLECTION_INTERFACE = "org.freedesktop.Secret.Collection"
        const val ITEM_INTERFACE = "org.freedesktop.Secret.Item"
        const val PROMPT_INTERFACE = "org.freedesktop.Secret.Prompt"
        const val NO_OBJECT = "/"
        const val DEFAULT_ALIAS = "default"
        const val DEFAULT_COLLECTION_LABEL = "Default keyring"
        const val DISMISSED = "app.katari.desktop.secrets.Dismissed"

        const val ATTRIBUTE_APPLICATION = "application"
        const val ATTRIBUTE_NAME = "name"

        // Long enough for the user to type the keyring's password
        val PROMPT_TIMEOUT = 5.minutes
    }
}
