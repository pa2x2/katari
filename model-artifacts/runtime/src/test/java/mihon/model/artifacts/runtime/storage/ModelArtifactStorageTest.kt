package mihon.model.artifacts.runtime.storage

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.runtime.artifactDescriptor
import mihon.model.artifacts.runtime.artifactFile
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ModelArtifactStorageTest {

    @TempDir
    lateinit var root: File

    private val content = "weights".toByteArray()
    private val descriptor = artifactDescriptor(artifactFile("model.onnx", content))

    @Test
    fun `an artifact is installed only after it was committed`() {
        val storage = ModelArtifactStorage { root }
        storage.begin(descriptor)
        storage.target(descriptor, descriptor.files.single()).writeBytes(content)

        storage.installed(descriptor) shouldBe null
        storage.stored().single().complete shouldBe false

        storage.commit(descriptor)

        storage.installed(descriptor) shouldNotBe null
        storage.stored().single().complete shouldBe true
    }

    @Test
    fun `a stored revision is not installed for a descriptor that declares different content`() {
        val storage = ModelArtifactStorage { root }
        install(storage, descriptor)
        val changed = artifactDescriptor(artifactFile("model.onnx", "other".toByteArray()))

        storage.installed(changed) shouldBe null
    }

    @Test
    fun `committing a revision removes the other revisions of the same artifact`() {
        val storage = ModelArtifactStorage { root }
        val next = artifactDescriptor(artifactFile("model.onnx", content), revision = "r2")
        val unrelated = artifactDescriptor(artifactFile("model.onnx", content), id = "other.model")
        install(storage, descriptor)
        install(storage, unrelated)

        install(storage, next)

        storage.installed(descriptor) shouldBe null
        storage.stored().map { it.descriptor } shouldContainExactly listOf(unrelated, next)
    }

    private fun install(storage: ModelArtifactStorage, descriptor: ModelArtifactDescriptor) {
        storage.begin(descriptor)
        storage.target(descriptor, descriptor.files.single()).writeBytes(content)
        storage.commit(descriptor)
    }
}
