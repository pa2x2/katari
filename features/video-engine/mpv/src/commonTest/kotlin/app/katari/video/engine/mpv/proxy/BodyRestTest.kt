package app.katari.video.engine.mpv.proxy

import io.kotest.matchers.shouldBe
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Test

class BodyRestTest {

    private val segment = Request.Builder()
        .url("https://host.example/video.mp4")
        .header("Range", "bytes=1000-1999")
        .build()

    private fun response(request: Request, code: Int, length: Int, vararg headers: Pair<String, String>) =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("")
            .apply { headers.forEach { (name, value) -> header(name, value) } }
            .body(ByteArray(length).toResponseBody())
            .build()

    // A segment that is part of a file comes as a range of it: its rest starts where the copy stopped in the file, not
    // in the segment, or the host sends bytes mpv already got.
    @Test
    fun `the rest of a segment cut short is asked for from where it stopped in the file`() {
        val cut = response(segment, 206, 1000, "Content-Range" to "bytes 1000-1999/5000", "ETag" to "\"v1\"")

        val rest = cut.restRequest(copied = 300)!!

        rest.header("Range") shouldBe "bytes=1300-1999"
        rest.header("If-Range") shouldBe "\"v1\""
    }

    // A host whose file changed answers If-Range with the whole new file, and one that ignores ranges with the whole
    // file: either spliced in would hand mpv the file's start as the segment's rest.
    @Test
    fun `only the exact range asked for is taken as the rest`() {
        val rest = response(segment, 206, 1000, "Content-Range" to "bytes 1000-1999/5000").restRequest(copied = 300)!!

        response(rest, 206, 700, "Content-Range" to "bytes 1300-1999/5000").continues(rest) shouldBe true
        response(rest, 200, 5000).continues(rest) shouldBe false
        response(rest, 206, 1000, "Content-Range" to "bytes 1000-1999/5000").continues(rest) shouldBe false
    }
}
