package mihon.extension.compat.webkit

import android.webkit.JavascriptInterface
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * The objects a web view gives its pages' scripts (`addJavascriptInterface`): their public methods marked
 * [JavascriptInterface], called by name and number of arguments in the order the page called them, on a thread of
 * their own, as Android's web view calls them on its JavaBridge thread. A `String` parameter takes a string and is null
 * for any other value, a number parameter takes a number, rounded towards zero for a whole type, and a `boolean` takes
 * a boolean; what the method returns goes nowhere.
 */
internal class JavascriptBridges {
    private val objects = ConcurrentHashMap<String, Any>()

    /** Adds [target] as [name], replacing what [name] was; the names of the methods its scripts can call. */
    fun add(name: String, target: Any): Set<String> {
        objects[name] = target
        return bridgedMethods(target).mapTo(LinkedHashSet()) { it.name }
    }

    fun remove(name: String) {
        objects.remove(name)
    }

    /** Calls [method] of the object [name] with [arguments], a JSON array; nothing where there's no such method. */
    fun call(name: String, method: String, arguments: String) {
        val target = objects[name] ?: return
        val values = runCatching { Json.parseToJsonElement(arguments).jsonArray }.getOrNull() ?: return
        val callable = bridgedMethods(target).firstOrNull { it.name == method && it.parameterCount == values.size }
        if (callable == null) {
            logcat(LogPriority.WARN) { "A page called $name.$method, which takes no ${values.size} arguments" }
            return
        }
        bridgeThread.execute { invoke(target, callable, values) }
    }

    private fun invoke(target: Any, method: Method, values: JsonArray) {
        val arguments = method.parameterTypes.zip(values) { type, value -> value.toArgument(type) }
        try {
            method.invoke(target, *arguments.toTypedArray())
        } catch (e: InvocationTargetException) {
            logcat(LogPriority.ERROR, e.targetException) { "A web view's ${method.name} failed for its page" }
        } catch (e: ReflectiveOperationException) {
            logcat(LogPriority.ERROR, e) { "A web view's ${method.name} couldn't be called for its page" }
        }
    }

    private fun bridgedMethods(target: Any): List<Method> = target.javaClass.methods.filter { method ->
        method.isAnnotationPresent(JavascriptInterface::class.java) && !Modifier.isStatic(method.modifiers)
    }

    private fun JsonElement.toArgument(type: Class<*>): Any? {
        val primitive = this as? JsonPrimitive
        val number = primitive?.takeUnless { it.isString }?.doubleOrNull ?: 0.0
        return when (type) {
            String::class.java -> primitive?.takeIf { it.isString }?.content
            java.lang.Boolean.TYPE -> primitive?.takeUnless { it.isString }?.booleanOrNull ?: false
            java.lang.Double.TYPE -> number
            java.lang.Float.TYPE -> number.toFloat()
            java.lang.Long.TYPE -> number.toLong()
            java.lang.Integer.TYPE -> number.toInt()
            java.lang.Short.TYPE -> number.toInt().toShort()
            java.lang.Byte.TYPE -> number.toInt().toByte()
            Character.TYPE -> number.toInt().toChar()
            else -> null
        }
    }

    private companion object {
        val bridgeThread = Executors.newSingleThreadExecutor { task ->
            Thread(task, "JavaBridge").apply { isDaemon = true }
        }
    }
}
