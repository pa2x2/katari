package app.katari.telemetry.freeze

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MainThreadStackTest {
    @Test
    fun `a freeze carries the main thread's Java frames and not another thread's`() {
        val trace = """
            ----- pid 8213 at 2026-11-09 14:02:11.482913517+0100 -----
            Cmd line: app.katari
            Build fingerprint: 'google/bluejay/bluejay:17/CP1A.261005.003/14218761:user/release-keys'
            ABI: 'arm64'

            "Signal Catcher" daemon prio=10 tid=2 Runnable
              | group="system" sCount=0 ucsCount=0 flags=0 obj=0x15340228 self=0xb400007b2c8a1380
              at dalvik.system.VMStack.getThreadStackTrace(Native method)

            "main" prio=5 tid=1 Blocked
              | group="main" sCount=1 ucsCount=0 flags=1 obj=0x72c1e9b0 self=0xb400007b2c87d7b0
              | sysTid=8213 nice=-10 cgrp=top-app sched=0/0 handle=0x7d8e3214f8
              | held mutexes=
              at app.katari.library.LibraryCache.load(LibraryCache.kt:48)
              - waiting to lock <0x0e9a2b1c> (a java.lang.Object) held by thread 24
              at app.katari.library.LibraryScreenModel${'$'}refresh${'$'}1.invokeSuspend(Unknown Source:12)
              at java.lang.Object.wait(Native method)
              at a.b.c(SourceFile)
              native: #00 pc 000000000008f0d8  /apex/com.android.runtime/lib64/bionic/libc.so (__futex_wait+8)
              at android.os.Handler.handleCallback(Handler.java:991)

            "DefaultDispatcher-worker-1" daemon prio=5 tid=24 Runnable
              at app.katari.library.LibraryCache.write(LibraryCache.kt:90)
        """.trimIndent()

        mainThreadStack(trace) shouldBe listOf(
            StackTraceElement("app.katari.library.LibraryCache", "load", "LibraryCache.kt", 48),
            StackTraceElement("app.katari.library.LibraryScreenModel\$refresh\$1", "invokeSuspend", null, 12),
            StackTraceElement("java.lang.Object", "wait", null, -2),
            StackTraceElement("a.b", "c", "SourceFile", -1),
            StackTraceElement("android.os.Handler", "handleCallback", "Handler.java", 991),
        )
    }
}
