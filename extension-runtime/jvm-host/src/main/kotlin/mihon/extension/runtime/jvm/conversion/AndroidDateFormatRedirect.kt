package mihon.extension.runtime.jvm.conversion

import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.commons.SimpleRemapper

/**
 * Points an extension's uses of `java.text.SimpleDateFormat` at the Android stand-ins' ICU-backed one, which parses and
 * formats dates as Android's ICU-based class does: the JDK's differs in month and weekday names, era and time-zone
 * text for many locales, so a source's date patterns would parse nothing.
 *
 * Only `SimpleDateFormat` is redirected, as Suwayomi does. The stand-ins' `Calendar`, `TimeZone` and `NumberFormat`
 * take and return ICU types where the JDK's take its own, so a call redirected to them would find no such method.
 */
internal object AndroidDateFormatRedirect {

    val remapper: Remapper = SimpleRemapper(
        Opcodes.ASM9,
        mapOf("java/text/SimpleDateFormat" to "xyz/nulldev/androidcompat/replace/java/text/SimpleDateFormat"),
    )
}
