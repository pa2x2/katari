package mihon.core.migration

import uy.kohesive.injekt.Injekt

class MigrationContext(
    val dryrun: Boolean,
    val previousVersion: Int,
    @PublishedApi internal val dependencies: Map<Class<*>, Any> = emptyMap(),
) {

    inline fun <reified T> get(): T? {
        @Suppress("UNCHECKED_CAST")
        return dependencies[T::class.java] as? T ?: Injekt.getInstanceOrNull(T::class.java)
    }
}
