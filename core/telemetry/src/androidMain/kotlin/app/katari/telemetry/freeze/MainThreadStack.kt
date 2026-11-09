package app.katari.telemetry.freeze

/**
 * The main thread's frames in an ANR trace, the dump of every thread Android writes when an app stops responding. A
 * thread's part starts with its quoted name and ends at a blank line, and its Java frames are the lines that start with
 * `at`; the lines between them describe the thread, the locks it waits on and its native frames.
 *
 * ```
 * "main" prio=5 tid=1 Blocked
 *   | group="main" sCount=1 ucsCount=0 flags=1 obj=0x72c1e9b0 self=0xb400007b2c87d7b0
 *   at app.katari.Example.load(Example.kt:12)
 *   - waiting to lock <0x0e9a2b1c> (a java.lang.Object) held by thread 24
 *   at android.os.Handler.handleCallback(Handler.java:991)
 * ```
 */
internal fun mainThreadStack(trace: String): List<StackTraceElement> = trace.lineSequence()
    .dropWhile { !it.startsWith("\"main\" ") }
    .drop(1)
    .takeWhile { it.isNotBlank() }
    .mapNotNull { FRAME.matchEntire(it.trim())?.destructured }
    .map { (className, method, location) -> frame(className, method, location) }
    .toList()

private fun frame(className: String, method: String, location: String): StackTraceElement {
    if (location == NATIVE_METHOD) return StackTraceElement(className, method, null, NATIVE_LINE)
    val file = location.substringBefore(':').takeUnless { it == UNKNOWN_SOURCE }
    val line = location.substringAfter(':', "").toIntOrNull() ?: UNKNOWN_LINE
    return StackTraceElement(className, method, file, line)
}

private val FRAME = Regex("""at (.+)\.([^.(]+)\((.*)\)""")

private const val NATIVE_METHOD = "Native method"
private const val UNKNOWN_SOURCE = "Unknown Source"

// What StackTraceElement takes for a native method's line and for a line that isn't known
private const val NATIVE_LINE = -2
private const val UNKNOWN_LINE = -1
