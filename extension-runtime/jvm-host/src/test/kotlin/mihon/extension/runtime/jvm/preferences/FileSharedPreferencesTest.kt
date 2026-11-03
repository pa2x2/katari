package mihon.extension.runtime.jvm.preferences

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tachiyomi.core.common.preference.DesktopPreferenceStore
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

class FileSharedPreferencesTest {

    @TempDir
    lateinit var directory: Path

    // Extensions apply from their own threads, often several editors at once. An editor that wrote back what it read
    // instead of only its own changes would drop another's apply, and the loss would only show as a setting an
    // extension forgets after the next start.
    @Test
    fun `editors applying at once from several threads all keep their changes after a restart`() {
        val file = directory.resolve("source_1.json")
        val preferences = FileSharedPreferences(DesktopPreferenceStore(file), listenerThread = Runnable::run)
        val start = CountDownLatch(1)
        val writers = (0 until THREADS).map { writer ->
            thread {
                start.await()
                repeat(KEYS_PER_THREAD) { key ->
                    preferences.edit().putLong("$writer-$key", key.toLong()).apply()
                }
            }
        }
        start.countDown()
        writers.forEach { it.join() }
        preferences.edit().commit() shouldBe true

        val restarted = FileSharedPreferences(DesktopPreferenceStore(file), listenerThread = Runnable::run)
        restarted.all.size shouldBe THREADS * KEYS_PER_THREAD
        restarted.getLong("${THREADS - 1}-${KEYS_PER_THREAD - 1}", -1) shouldBe KEYS_PER_THREAD - 1L
    }

    private companion object {
        const val THREADS = 8
        const val KEYS_PER_THREAD = 50
    }
}
