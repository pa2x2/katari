package mihon.text.recognition.api.image

import java.io.InputStream
import java.security.MessageDigest

@JvmInline
value class ImageContentKey(
    val value: String,
) {
    init {
        require(value.isNotBlank())
    }

    companion object {
        /** Identifies encoded image bytes by their SHA-256 digest. The stream is read to its end but not closed. */
        fun sha256(input: InputStream): ImageContentKey {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
            return ImageContentKey(digest.digest().joinToString("") { "%02x".format(it) })
        }
    }
}
