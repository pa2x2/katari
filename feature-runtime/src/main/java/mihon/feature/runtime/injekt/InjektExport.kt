package mihon.feature.runtime.injekt

import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.TypeReference
import uy.kohesive.injekt.api.fullType

/**
 * A graph binding that code outside the graph still resolves through Injekt. It is registered at startup and
 * resolves the graph's instance on first use.
 */
class InjektExport<T : Any>(
    private val type: TypeReference<T>,
    private val instance: () -> T,
) {
    fun registerIn(registrar: InjektRegistrar) {
        registrar.addSingletonFactory(type, instance)
    }
}

inline fun <reified T : Any> injektExport(noinline instance: () -> T): InjektExport<T> =
    InjektExport(fullType(), instance)
