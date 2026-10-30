package mihon.gradle.platformparity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlatformPlacementRulesTest {

    @Test
    fun `a worker that holds its own loop or helpers is reported, one that adapts a shared runner is not`() {
        val findings = checkPlatformPlacement(
            listOf(
                PlatformSource(
                    "app/src/main/java/app/work/WalkJob.kt",
                    """
                        package app.work

                        class WalkJob(private val context: Context, params: WorkerParameters) :
                            CoroutineWorker(context, params) {
                            @Inject
                            private lateinit var walks: WalkRepository

                            override suspend fun doWork(): Result {
                                walks.getAll().forEach { walk(it) }
                                return Result.success()
                            }

                            private suspend fun walk(entry: Entry) {
                                while (entry.hasMore()) entry.next()
                            }

                            private companion object {
                                const val MAX_ATTEMPTS = 8
                                val running = mutableMapOf<String, Job>()
                            }
                        }
                    """.trimIndent(),
                ),
                PlatformSource(
                    "app/src/main/java/app/work/ThinJob.kt",
                    """
                        package app.work

                        /** Runs the shared walk; "for (each) entry" in a comment or string is no loop. */
                        class ThinJob(private val context: Context, params: WorkerParameters) :
                            CoroutineWorker(context, params) {
                            @Inject
                            private lateinit var walk: WalkPendingChapterLists

                            private val notifier = WalkNotifier(context)

                            init {
                                context.appGraph.inject(this)
                            }

                            override suspend fun doWork(): Result {
                                logcat { "for (each) entry" }
                                return when (walk.await(runAttemptCount)) {
                                    WorkOutcome.Done -> Result.success()
                                    else -> Result.retry()
                                }
                            }

                            companion object {
                                internal const val KEY_PROFILE = "profile"
                            }
                        }
                    """.trimIndent(),
                ),
                PlatformSource(
                    "app/src/test/java/app/work/TestJob.kt",
                    """
                        class TestJob(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
                            private fun helper() = listOf(1).map { it }
                        }
                    """.trimIndent(),
                ),
            ),
            buildFiles = emptyList(),
            apis = PlatformApis.EMPTY,
        )

        assertEquals(
            listOf(
                "app/src/main/java/app/work/WalkJob.kt:9: WalkJob loops (forEach); move the loop into the shared " +
                    "runner the worker calls",
                "app/src/main/java/app/work/WalkJob.kt:13: WalkJob declares the function walk; move it into the " +
                    "shared runner the worker calls",
                "app/src/main/java/app/work/WalkJob.kt:14: WalkJob loops (while); move the loop into the shared " +
                    "runner the worker calls",
                "app/src/main/java/app/work/WalkJob.kt:19: WalkJob's companion holds more than const values; keep " +
                    "state and helpers in shared code",
            ),
            findings.map { "${it.relativePath}:${it.lineNumber}: ${it.reason}" },
        )
    }

    @Test
    fun `an Android class answering a shared contract with nothing only Android has is reported`() {
        val findings = checkPlatformPlacement(
            listOf(
                PlatformSource("feature/src/commonMain/kotlin/feature/Clock.kt", "package feature\n\ninterface Clock"),
                PlatformSource(
                    "feature/src/commonMain/kotlin/feature/Vibration.kt",
                    """
                        package feature

                        @PlatformFacility
                        interface Vibration
                    """.trimIndent(),
                ),
                PlatformSource(
                    "app/src/main/java/app/WallClock.kt",
                    """
                        package app

                        import feature.Clock

                        class WallClock : Clock
                    """.trimIndent(),
                ),
                PlatformSource(
                    "app/src/main/java/app/Uptime.kt",
                    """
                        package app

                        import android.os.SystemClock

                        internal fun uptime(): Long = SystemClock.uptimeMillis()
                    """.trimIndent(),
                ),
                // Bound through the helper beside it, which needs Android.
                PlatformSource(
                    "app/src/main/java/app/UptimeClock.kt",
                    """
                        package app

                        import feature.Clock

                        class UptimeClock : Clock {
                            fun now() = uptime()
                        }
                    """.trimIndent(),
                ),
                PlatformSource(
                    "app/src/main/java/app/NoVibration.kt",
                    """
                        package app

                        import feature.Vibration

                        object NoVibration : Vibration
                    """.trimIndent(),
                ),
            ),
            buildFiles = APP_BUILD_FILES,
            apis = PlatformApis(
                desktop = PlatformApiNames.EMPTY,
                android = PlatformApiNames(classes = setOf("android.os.SystemClock"), callables = emptySet()),
                androidStandIns = PlatformApiNames.EMPTY,
            ),
        )

        assertEquals(
            listOf(
                "app/src/main/java/app/WallClock.kt:5: WallClock implements the shared feature.Clock but needs " +
                    "nothing only Android has; move WallClock.kt to commonMain",
            ),
            findings.map { "${it.relativePath}:${it.lineNumber}: ${it.reason}" },
        )
    }

    @Test
    fun `a desktop binding that names nothing only desktop has is reported`() {
        val findings = checkPlatformPlacement(
            listOf(
                PlatformSource(
                    "feature/src/commonMain/kotlin/feature/Clock.kt",
                    """
                        package feature

                        interface Clock

                        interface Tray

                        class SystemClock : Clock
                    """.trimIndent(),
                ),
                PlatformSource(
                    "desktop/src/main/kotlin/desktop/DesktopTray.kt",
                    "package desktop\n\nclass DesktopTray : feature.Tray",
                ),
                PlatformSource(
                    "desktop/src/main/kotlin/desktop/DesktopBindings.kt",
                    """
                        package desktop

                        import feature.Clock
                        import feature.SystemClock
                        import feature.Tray
                        import javax.swing.JFrame

                        @BindingContainer
                        interface DesktopBindings {
                            @Binds
                            val SystemClock.clock: Clock

                            @Binds
                            val DesktopTray.tray: Tray

                            companion object {
                                @Provides
                                fun frame(): JFrame = JFrame()
                            }
                        }
                    """.trimIndent(),
                ),
            ),
            buildFiles = APP_BUILD_FILES,
            apis = PlatformApis(
                desktop = PlatformApiNames(classes = setOf("javax.swing.JFrame"), callables = emptySet()),
                android = PlatformApiNames.EMPTY,
                androidStandIns = PlatformApiNames.EMPTY,
            ),
        )

        assertEquals(
            listOf(
                "desktop/src/main/kotlin/desktop/DesktopBindings.kt:10: this binding names nothing only desktop " +
                    "has; provide it from a shared container",
            ),
            findings.map { "${it.relativePath}:${it.lineNumber}: ${it.reason}" },
        )
    }
}
