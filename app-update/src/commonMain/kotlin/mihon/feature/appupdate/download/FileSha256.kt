package mihon.feature.appupdate.download

import java.io.File
import java.security.MessageDigest

/** The file's SHA-256 digest as lowercase hex, which GitHub's asset digests are compared with. */
fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 8)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
