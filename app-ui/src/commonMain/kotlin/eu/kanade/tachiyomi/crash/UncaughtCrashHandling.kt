package eu.kanade.tachiyomi.crash

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/**
 * What happens to an exception no code catches: it is logged, [presenter] opens the crash screen with it, and the
 * handler installed before gets it as it would have without this one. The crash screen's process reads the exception
 * back with [decode].
 */
class UncaughtCrashHandling private constructor(
    private val presenter: CrashPresenter,
    private val previousHandler: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {

    /** Keeps only the stack trace, as the message of the [Throwable] read back, since exceptions don't serialize. */
    object ThrowableSerializer : KSerializer<Throwable> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("Throwable", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): Throwable =
            Throwable(message = decoder.decodeString())

        override fun serialize(encoder: Encoder, value: Throwable) =
            encoder.encodeString(value.stackTraceToString())
    }

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        logcat(priority = LogPriority.ERROR, throwable = exception)
        presenter.present(encode(exception))
        previousHandler?.uncaughtException(thread, exception)
    }

    companion object {
        fun install(presenter: CrashPresenter) {
            Thread.setDefaultUncaughtExceptionHandler(
                UncaughtCrashHandling(presenter, Thread.getDefaultUncaughtExceptionHandler()),
            )
        }

        fun encode(exception: Throwable): String = Json.encodeToString(ThrowableSerializer, exception)

        /** The exception in [report]; null, after logging why, when it can't be read. */
        fun decode(report: String?): Throwable? {
            return try {
                Json.decodeFromString(ThrowableSerializer, checkNotNull(report) { "No crash report" })
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Wasn't able to read the crashed exception" }
                null
            }
        }
    }
}
