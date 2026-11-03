package eu.kanade.tachiyomi.network

/**
 * Util for evaluating JavaScript in sources. Sources inject it; the app provides the engine behind it.
 */
abstract class JavaScriptEngine {

    /**
     * Evaluate arbitrary JavaScript code and get the result as a primtive type
     * (e.g., String, Int).
     *
     * @since extensions-lib 1.4
     * @param script JavaScript to execute.
     * @return Result of JavaScript code as a primitive type.
     */
    abstract suspend fun <T> evaluate(script: String): T
}
