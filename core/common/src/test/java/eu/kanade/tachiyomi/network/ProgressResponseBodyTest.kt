package eu.kanade.tachiyomi.network

import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProgressResponseBodyTest {

    @Test
    fun `progress includes bytes downloaded before resumed response`() {
        val updates = mutableListOf<ProgressUpdate>()
        val listener = object : ProgressListener {
            override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
                updates += ProgressUpdate(bytesRead, contentLength, done)
            }
        }
        val body = ProgressResponseBody("abc".toResponseBody(), listener, existingSize = 10L)

        body.source().readByteArray()

        assertTrue(updates.last().done)
        assertEquals(13L, updates.last().bytesRead)
    }
}

private data class ProgressUpdate(
    val bytesRead: Long,
    val contentLength: Long,
    val done: Boolean,
)
