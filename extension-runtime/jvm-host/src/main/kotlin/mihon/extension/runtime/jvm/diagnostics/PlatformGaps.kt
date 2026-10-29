package mihon.extension.runtime.jvm.diagnostics

/**
 * Recognises an error an extension raised because desktop provides only part of Android: a stand-in that throws
 * `"Stub!"`, or a class, method or field of Android's or AndroidX's that the stand-ins lack, which the JVM reports as a
 * `NoClassDefFoundError`, `NoSuchMethodError`, `NoSuchFieldError`, `AbstractMethodError` or `VerifyError`. The error
 * may be the cause of the one caught.
 */
object PlatformGaps {

    /** What's missing as `package.Class` or `package.Class.member`, `null` when [error] isn't such an error. */
    fun missingMember(error: Throwable): String? =
        generateSequence(error) { it.cause.takeIf { cause -> cause !== it } }
            .take(MAX_CAUSES)
            .firstNotNullOfOrNull(::missingMemberOf)

    private fun missingMemberOf(error: Throwable): String? {
        val message = error.message.orEmpty()
        return when (error) {
            is NoClassDefFoundError -> platformClass(message.replace('/', '.'))
            is NoSuchMethodError, is NoSuchFieldError, is AbstractMethodError -> quotedMember(message)
                ?: platformClass(message.replace('/', '.'))
            // Only the reason counts: the frame HotSpot prints after it lists every type in scope, Android's among them
            is VerifyError -> PLATFORM_TYPE.find(verifyReason(message))
                ?.value
                ?.replace('/', '.')
            else -> if (message == STUB_MESSAGE) stubMember(error) else null
        }
    }

    private fun verifyReason(message: String): String =
        message.substringAfter("Reason:", missingDelimiterValue = message.lineSequence().first())
            .trimStart()
            .lineSequence()
            .first()

    /** The stand-in method that threw: the first of the stack's frames that's in an Android class. */
    private fun stubMember(error: Throwable): String? {
        val frame = error.stackTrace.firstOrNull { frame -> isPlatform(frame.className) } ?: return null
        return if (frame.methodName == "<init>" || frame.methodName == "<clinit>") {
            frame.className
        } else {
            "${frame.className}.${frame.methodName}"
        }
    }

    /**
     * The member a HotSpot linkage message quotes, such as `'void android.graphics.Bitmap.setPixels(int[], …)'`
     * or `'int SDK_INT'` of `android.os.Build`, without its types.
     */
    private fun quotedMember(message: String): String? {
        val quoted = QUOTED.find(message)?.groupValues?.get(1) ?: return null
        val name = quoted.substringBefore('(').substringAfterLast(' ')
        if (isPlatform(name)) return name
        // A field's message names its class outside the quotes: "Class android.os.Build does not have member field …"
        val owner = OWNER.find(message)?.groupValues?.get(1)?.removeSuffix(".")?.takeIf(::isPlatform) ?: return null
        return "$owner.$name"
    }

    private fun platformClass(text: String): String? =
        text.split(' ', ',', ';', '\'').firstOrNull(::isPlatform)?.removeSuffix(".")

    private fun isPlatform(name: String): Boolean = PLATFORM_PACKAGES.any(name::startsWith)

    private const val STUB_MESSAGE = "Stub!"
    private const val MAX_CAUSES = 16
    private val PLATFORM_PACKAGES = listOf("android.", "androidx.", "dalvik.", "com.android.")
    private val PLATFORM_TYPE = Regex("(android|androidx|dalvik)/[\\w/$]+")
    private val QUOTED = Regex("'([^']+)'")
    private val OWNER = Regex("(?:class|interface) ((?:android|androidx|dalvik)\\.[\\w.$]+)", RegexOption.IGNORE_CASE)
}
