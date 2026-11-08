package app.katari.gradle.extensions

/** The JVM options of every process that runs the desktop extension host: the desktop app and its harness. */
val extensionHostJvmArguments: List<String> = listOf(
    // The extension host's Android stand-ins put android.os.Build on the classpath, from which kotlinx.coroutines' fast
    // service loader concludes it runs on Android and looks only for Android's main dispatcher, the main Looper. The
    // regular service loader finds the platform's.
    "-Dkotlinx.coroutines.fast.service.loader=false",
    // Every thread's stack. Android's java.util.regex is ICU's, which keeps its backtracking on the heap, up to 8 MB;
    // the JDK's recurses on the thread's stack instead, a few hundred bytes for each repetition of a group, so a
    // pattern such as ((?:[^']|\\.)*) over a page's 40 KB script string overflows the default 1 MB stack. Extension code
    // runs on whatever thread calls or resumes it (coroutine pools, OkHttp's, RxJava's, its own), so only the default
    // reaches all of them. Such a match over 40 KB took between 16 and 32 MB of stack, so 64 MB leaves room for larger
    // pages; it is address space, which a thread only fills as deep as it recurses.
    "-Xss64m",
)
