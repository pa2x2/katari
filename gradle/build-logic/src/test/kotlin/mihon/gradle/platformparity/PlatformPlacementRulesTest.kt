package mihon.gradle.platformparity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlatformPlacementRulesTest {

    @Test
    fun `a worker that holds its own loop or helpers is reported, one that adapts a shared runner is not`() {
        val findings = checkPlatformPlacement(
            listOf(
                PlatformPlacementSource(
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
                PlatformPlacementSource(
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
                PlatformPlacementSource(
                    "app/src/test/java/app/work/TestJob.kt",
                    """
                        class TestJob(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
                            private fun helper() = listOf(1).map { it }
                        }
                    """.trimIndent(),
                ),
            ),
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
}
