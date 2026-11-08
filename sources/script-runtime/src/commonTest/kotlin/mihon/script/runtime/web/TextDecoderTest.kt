package mihon.script.runtime.web

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import mihon.script.runtime.ScriptNetwork
import mihon.script.runtime.ScriptRuntimeFactory
import mihon.script.runtime.ScriptRuntimeSpec
import mihon.script.runtime.ScriptSource
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test

class TextDecoderTest {

    // quickjs-kt hands the host a typed array's whole buffer; protobufjs decodes strings from subarrays of a message,
    // and got the bytes around each string too
    @Test
    fun `a view on part of a buffer decodes only its own bytes`() = runBlocking<Unit> {
        val runtime = ScriptRuntimeFactory(ScriptNetwork(OkHttpClient()) { "test" }).open(
            ScriptRuntimeSpec(
                name = "text",
                scripts = listOf(
                    ScriptSource(
                        "text.js",
                        """
                        function decode() {
                          var bytes = new TextEncoder().encode('<hé>');
                          var decoder = new TextDecoder();
                          return decoder.decode(bytes.subarray(1, 4)) + decoder.decode(new DataView(bytes.buffer, 4, 1));
                        }
                        """.trimIndent(),
                    ),
                ),
            ),
        )

        runtime.use { it.call("decode", JsonArray(emptyList())) } shouldBe JsonPrimitive("hé>")
    }
}
