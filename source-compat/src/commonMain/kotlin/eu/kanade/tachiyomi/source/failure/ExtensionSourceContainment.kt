package eu.kanade.tachiyomi.source.failure

import eu.kanade.tachiyomi.source.entry.UnifiedSource
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.intercepted
import kotlin.coroutines.jvm.internal.CoroutineStackFrame
import kotlin.reflect.KClass

/*
 * Entry SDK sources are the extension's own classes, which the app calls with no adapter in between, unlike Tachiyomi
 * and Aniyomi sources, whose adapters run each call through runExtensionCode. So the app gets each Entry SDK source
 * behind a proxy that does the same for every member. The app tells a source's capabilities apart by the interfaces
 * it implements, which the proxy implements too; nothing in the app may check for or cast to a source's class.
 */

/**
 * [source] behind a proxy that implements each interface of its class the app can name and turns the errors
 * [isExtensionCodeError] names into [ExtensionCodeException]s, thrown or, for a suspending member, resumed with.
 * Every other exception reaches the caller as the extension threw it, except that the JVM hands on a checked exception
 * from a member that doesn't suspend inside an `UndeclaredThrowableException`, since a proxy's members declare none.
 * [source] already behind one comes back as it is.
 */
fun containExtensionCode(source: UnifiedSource): UnifiedSource {
    if (source.containedCalls != null) return source
    val appClasses = UnifiedSource::class.java.classLoader
    val interfaces = source.javaClass.allInterfaces().filter { it.isVisibleFrom(appClasses) }
    return Proxy.newProxyInstance(
        appClasses,
        interfaces.toTypedArray(),
        ContainedSourceCalls(source),
    ) as UnifiedSource
}

/** The class the source was written as, also for one behind [containExtensionCode], where the class is the proxy's. */
val UnifiedSource.implementationClass: KClass<*>
    get() = (containedCalls?.source ?: this)::class

private val UnifiedSource.containedCalls: ContainedSourceCalls?
    get() = if (Proxy.isProxyClass(javaClass)) Proxy.getInvocationHandler(this) as? ContainedSourceCalls else null

private fun Class<*>.allInterfaces(): Set<Class<*>> {
    val found = LinkedHashSet<Class<*>>()
    fun collect(type: Class<*>) {
        type.interfaces.forEach { if (found.add(it)) collect(it) }
        type.superclass?.let(::collect)
    }
    collect(this)
    return found
}

// The extension's own interfaces are left out: the app can't name them, and a proxy can implement only interfaces its
// class loader sees
private fun Class<*>.isVisibleFrom(loader: ClassLoader?): Boolean = Modifier.isPublic(modifiers) &&
    try {
        Class.forName(name, false, loader) == this
    } catch (_: ClassNotFoundException) {
        false
    }

/**
 * Calls [source]'s members, unwrapping the `InvocationTargetException` reflection puts around what they throw.
 *
 * Equality is [source]'s with the other side seen through its proxy, so a source still equals itself and keys a map as
 * it did. A suspending member can't throw a checked exception through the proxy before it suspends, so that exception
 * goes to the caller's continuation instead, through the caller's dispatcher as after a real suspension, and the call
 * reports itself suspended.
 */
private class ContainedSourceCalls(val source: UnifiedSource) : InvocationHandler {

    override fun invoke(proxy: Any, method: Method, args: Array<Any?>?): Any? {
        if (method.isEquals()) {
            val other = args!![0]
            return runExtensionCode { source == ((other as? UnifiedSource)?.containedCalls?.source ?: other) }
        }

        if (args == null || args.lastOrNull() !is Continuation<*>) {
            return try {
                method.invoke(source, *args.orEmpty())
            } catch (e: InvocationTargetException) {
                throw e.targetException.contained()
            }
        }

        @Suppress("UNCHECKED_CAST")
        val caller = args.last() as Continuation<Any?>
        args[args.lastIndex] = ContainedContinuation(caller)
        return try {
            method.invoke(source, *args)
        } catch (e: InvocationTargetException) {
            val failure = e.targetException.contained()
            if (failure is RuntimeException || failure is Error) throw failure
            caller.intercepted().resumeWith(Result.failure(failure))
            COROUTINE_SUSPENDED
        }
    }

    private fun Method.isEquals(): Boolean = declaringClass == Any::class.java && name == "equals"
}

/**
 * The continuation a suspending member is called with, so an error it resumes with after suspending is contained. It is
 * a stack frame so that the debugger and kotlinx.coroutines' stack trace recovery still walk on to the caller.
 */
private class ContainedContinuation(private val caller: Continuation<Any?>) : Continuation<Any?>, CoroutineStackFrame {

    override val context: CoroutineContext get() = caller.context

    override val callerFrame: CoroutineStackFrame? get() = caller as? CoroutineStackFrame

    override fun getStackTraceElement(): StackTraceElement? = null

    override fun resumeWith(result: Result<Any?>) {
        caller.resumeWith(result.exceptionOrNull()?.let { Result.failure(it.contained()) } ?: result)
    }
}

private fun Throwable.contained(): Throwable =
    if (this is Error && isExtensionCodeError(this)) extensionCodeFailure(this) else this
