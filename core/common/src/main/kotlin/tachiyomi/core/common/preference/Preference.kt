package tachiyomi.core.common.preference

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

interface Preference<T> {

    fun key(): String

    fun get(): T

    fun set(value: T)

    fun isSet(): Boolean

    fun delete()

    fun defaultValue(): T

    fun changes(): Flow<T>

    fun stateIn(scope: CoroutineScope): StateFlow<T>

    companion object {
        /**
         * A preference that should not be exposed in places like backups without user consent.
         */
        fun isPrivate(key: String): Boolean {
            return key.startsWith(PRIVATE_PREFIX)
        }

        fun stripPrivateKey(key: String): String {
            return key.removePrefix(PRIVATE_PREFIX)
        }

        fun privateKey(key: String): String {
            return "$PRIVATE_PREFIX$key"
        }

        /**
         * A preference used for internal app state that isn't really a user preference
         * and therefore should not be in places like backups.
         */
        fun isAppState(key: String): Boolean {
            return key.startsWith(APP_STATE_PREFIX)
        }

        fun stripAppStateKey(key: String): String {
            return key.removePrefix(APP_STATE_PREFIX)
        }

        fun appStateKey(key: String): String {
            return "$APP_STATE_PREFIX$key"
        }

        private const val APP_STATE_PREFIX = "__APP_STATE_"
        private const val PRIVATE_PREFIX = "__PRIVATE_"
    }
}

inline fun <reified T, R : T> Preference<T>.getAndSet(crossinline block: (T) -> R) = set(
    block(get()),
)

operator fun <T> Preference<Set<T>>.plusAssign(item: T) {
    set(get() + item)
}

operator fun <T> Preference<Set<T>>.plusAssign(items: Iterable<T>) {
    set(get() + items)
}

operator fun <T> Preference<Set<T>>.minusAssign(item: T) {
    set(get() - item)
}

fun Preference<Boolean>.toggle(): Boolean {
    set(!get())
    return get()
}

fun Preference<Int>.coerceIn(range: IntRange): Preference<Int> {
    val delegate = this
    return object : Preference<Int> {
        override fun key(): String = delegate.key()

        override fun get(): Int = delegate.get().coerceIn(range)

        override fun set(value: Int) {
            delegate.set(value.coerceIn(range))
        }

        override fun isSet(): Boolean = delegate.isSet()

        override fun delete() = delegate.delete()

        override fun defaultValue(): Int = delegate.defaultValue().coerceIn(range)

        override fun changes(): Flow<Int> = delegate.changes().map { it.coerceIn(range) }

        override fun stateIn(scope: CoroutineScope): StateFlow<Int> {
            return changes().stateIn(scope, SharingStarted.Eagerly, get())
        }
    }
}
