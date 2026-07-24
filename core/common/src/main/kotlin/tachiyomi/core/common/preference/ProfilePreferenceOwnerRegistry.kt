package tachiyomi.core.common.preference

import kotlinx.coroutines.CoroutineScope

@JvmInline
value class ProfilePreferenceOwnerId(val value: String) {
    init {
        require(value.isNotBlank()) { "Profile preference owner ID must not be blank" }
    }
}

@JvmInline
value class ProfilePreferenceOwnerGroupId(val value: String) {
    init {
        require(value.isNotBlank()) { "Profile preference owner group ID must not be blank" }
    }
}

sealed interface ProfilePreferenceKeyPattern {
    fun matches(key: String): Boolean

    data class Prefix(val value: String) : ProfilePreferenceKeyPattern {
        init {
            require(value.isNotEmpty()) { "Profile preference key prefix must not be empty" }
        }

        override fun matches(key: String): Boolean = key.startsWith(value)

        fun key(suffix: Any): String = "$value$suffix"
    }
}

data class ProfilePreferenceOwnership(
    val profileKeys: Set<String>,
    val appStateKeys: Set<String>,
    val privateKeys: Set<String>,
)

internal class RegisteredProfilePreferenceOwner<T : Any>(
    val id: ProfilePreferenceOwnerId,
    private val factory: (PreferenceStore) -> T,
) {
    fun create(preferenceStore: PreferenceStore): T = factory(preferenceStore)
}

class InstalledProfilePreferenceOwner<T : Any> internal constructor(
    private val owner: RegisteredProfilePreferenceOwner<T>,
    private val preferenceStore: () -> PreferenceStore,
) {
    fun create(): T = owner.create(preferenceStore())
}

/** Binds discovered owner factories to the real profile-aware store used by one runtime installation boundary. */
class ProfilePreferenceOwnerInstaller(
    private val owners: ProfilePreferenceOwnerRegistry,
    private val preferenceStore: () -> PreferenceStore,
) {
    fun <T : Any> register(
        id: ProfilePreferenceOwnerId,
        keyPatterns: Set<ProfilePreferenceKeyPattern> = emptySet(),
        groups: Set<ProfilePreferenceOwnerGroupId> = emptySet(),
        factory: (PreferenceStore) -> T,
    ): InstalledProfilePreferenceOwner<T> {
        return InstalledProfilePreferenceOwner(
            owner = owners.register(id, keyPatterns, groups, factory),
            preferenceStore = preferenceStore,
        )
    }
}

/**
 * Installation-time discovery of preference owners whose state follows an application profile.
 *
 * One registration owns both runtime construction and ownership discovery. Static keys are derived from the same
 * factory that creates the real owner; patterns are reserved for key families whose suffixes only exist at runtime.
 */
class ProfilePreferenceOwnerRegistry {
    private val registrations = linkedMapOf<ProfilePreferenceOwnerId, Registration<*>>()
    private var sealed = false

    @Synchronized
    internal fun <T : Any> register(
        id: ProfilePreferenceOwnerId,
        keyPatterns: Set<ProfilePreferenceKeyPattern> = emptySet(),
        groups: Set<ProfilePreferenceOwnerGroupId> = emptySet(),
        factory: (PreferenceStore) -> T,
    ): RegisteredProfilePreferenceOwner<T> {
        check(!sealed) {
            "Profile preference ownership is already sealed; ${id.value} was installed too late"
        }
        check(id !in registrations) { "Duplicate profile preference owner: ${id.value}" }
        val overlappingAddedPatterns = keyPatterns.toList().let { patterns ->
            patterns.indices.firstNotNullOfOrNull { index ->
                patterns.drop(index + 1).firstOrNull(patterns[index]::overlaps)
            }
        }
        check(overlappingAddedPatterns == null) {
            "Profile preference owner ${id.value} declares overlapping key patterns"
        }
        val overlappingPattern = registrations.values
            .flatMap { registration -> registration.keyPatterns.map { registration.id to it } }
            .firstOrNull { (_, existing) ->
                keyPatterns.any { added -> existing.overlaps(added) }
            }
        check(overlappingPattern == null) {
            "Ambiguous profile preference key patterns from ${overlappingPattern?.first?.value} and ${id.value}"
        }
        val owner = RegisteredProfilePreferenceOwner(id, factory)
        registrations[id] = Registration(owner, keyPatterns, groups)
        return owner
    }

    @Synchronized
    fun ownership(
        existingKeys: Set<String> = emptySet(),
        group: ProfilePreferenceOwnerGroupId? = null,
    ): ProfilePreferenceOwnership {
        sealed = true
        val allOwnersByKey = resolveKeys(registrations.values, existingKeys)
        val conflicts = allOwnersByKey.filterValues { it.size > 1 }
        check(conflicts.isEmpty()) {
            "Profile preference keys have multiple owners: " + conflicts.entries.joinToString { (key, owners) ->
                "$key=${owners.joinToString { it.value }}"
            }
        }
        val selectedRegistrations = registrations.values.filter { registration ->
            group == null || group in registration.groups
        }
        return resolveKeys(selectedRegistrations, existingKeys).keys.toSet().toOwnership()
    }

    private fun resolveKeys(
        selectedRegistrations: Collection<Registration<*>>,
        existingKeys: Set<String>,
    ): Map<String, Set<ProfilePreferenceOwnerId>> {
        val ownersByKey = linkedMapOf<String, MutableSet<ProfilePreferenceOwnerId>>()
        val staticKeys = selectedRegistrations.flatMapTo(linkedSetOf()) { it.staticKeys }
        selectedRegistrations.forEach { registration ->
            registration.staticKeys.forEach { key ->
                ownersByKey.getOrPut(key, ::linkedSetOf) += registration.id
            }
            registration.keyPatterns.forEach { pattern ->
                (existingKeys + staticKeys).filter(pattern::matches).forEach { key ->
                    ownersByKey.getOrPut(key, ::linkedSetOf) += registration.id
                }
            }
        }
        return ownersByKey
    }

    private class Registration<T : Any>(
        private val owner: RegisteredProfilePreferenceOwner<T>,
        val keyPatterns: Set<ProfilePreferenceKeyPattern>,
        val groups: Set<ProfilePreferenceOwnerGroupId>,
    ) {
        val id: ProfilePreferenceOwnerId
            get() = owner.id

        val staticKeys: Set<String> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            KeyRecordingPreferenceStore().let { recorder ->
                owner.create(recorder)
                recorder.keys
            }
        }
    }
}

private fun ProfilePreferenceKeyPattern.overlaps(other: ProfilePreferenceKeyPattern): Boolean {
    return when {
        this is ProfilePreferenceKeyPattern.Prefix && other is ProfilePreferenceKeyPattern.Prefix ->
            value.startsWith(other.value) || other.value.startsWith(value)
        else -> false
    }
}

private fun Set<String>.toOwnership(): ProfilePreferenceOwnership {
    val profileKeys = linkedSetOf<String>()
    val appStateKeys = linkedSetOf<String>()
    val privateKeys = linkedSetOf<String>()
    forEach { key ->
        when {
            Preference.isPrivate(key) -> privateKeys += Preference.stripPrivateKey(key)
            Preference.isAppState(key) -> appStateKeys += Preference.stripAppStateKey(key)
            else -> profileKeys += key
        }
    }
    return ProfilePreferenceOwnership(profileKeys, appStateKeys, privateKeys)
}

private class KeyRecordingPreferenceStore : PreferenceStore {
    val keys = linkedSetOf<String>()

    override fun getString(key: String, defaultValue: String): Preference<String> = record(key, defaultValue)

    override fun getLong(key: String, defaultValue: Long): Preference<Long> = record(key, defaultValue)

    override fun getInt(key: String, defaultValue: Int): Preference<Int> = record(key, defaultValue)

    override fun getFloat(key: String, defaultValue: Float): Preference<Float> = record(key, defaultValue)

    override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> = record(key, defaultValue)

    override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> = record(
        key,
        defaultValue,
    )

    override fun <T> getObjectFromString(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ): Preference<T> = record(key, defaultValue)

    override fun <T> getObjectFromInt(
        key: String,
        defaultValue: T,
        serializer: (T) -> Int,
        deserializer: (Int) -> T,
    ): Preference<T> = record(key, defaultValue)

    override fun <T> getObjectSetFromStringSet(
        key: String,
        defaultValue: Set<T>,
        serializer: (T) -> String,
        deserializer: (String) -> T?,
    ): Preference<Set<T>> = record(key, defaultValue)

    override fun getAll(): Map<String, *> = emptyMap<String, Any?>()

    private fun <T> record(key: String, defaultValue: T): Preference<T> {
        keys += key
        return RecordedPreference(key, defaultValue)
    }
}

private class RecordedPreference<T>(
    private val key: String,
    private val defaultValue: T,
) : Preference<T> {
    override fun key(): String = key

    override fun get(): T = defaultValue

    override fun set(value: T) = Unit

    override fun isSet(): Boolean = false

    override fun delete() = Unit

    override fun defaultValue(): T = defaultValue

    override fun changes() = throw UnsupportedOperationException("Ownership-discovery preference")

    override fun stateIn(scope: CoroutineScope) = throw UnsupportedOperationException("Ownership-discovery preference")
}
