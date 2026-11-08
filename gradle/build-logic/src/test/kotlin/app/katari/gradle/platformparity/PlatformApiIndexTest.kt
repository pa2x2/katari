package app.katari.gradle.platformparity

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.io.File
import kotlin.metadata.KmClassifier
import kotlin.metadata.KmPackage
import kotlin.metadata.KmProperty
import kotlin.metadata.KmType
import kotlin.metadata.jvm.JvmMetadataVersion
import kotlin.metadata.jvm.KotlinClassMetadata

class PlatformApiIndexTest {

    // Desktop's Compose declares androidx.compose.ui.platform too, so only the facade's metadata tells that
    // LocalContext is Android's; indexed by class alone, Android code using it would pass as neutral.
    @Test
    fun `a top-level property of a Kotlin file facade is indexed under its package`(@TempDir classes: File) {
        val facade = "androidx/compose/ui/platform/AndroidCompositionLocals_androidKt"
        val localContext = KmProperty("LocalContext").apply {
            returnType =
                KmType().apply {
                    classifier = KmClassifier.Class("androidx/compose/runtime/ProvidableCompositionLocal")
                }
        }
        val metadata = KotlinClassMetadata.FileFacade(
            KmPackage().apply { properties += localContext },
            JvmMetadataVersion.LATEST_STABLE_SUPPORTED,
            0,
        ).write()
        val bytes = ClassWriter(0).apply {
            visit(Opcodes.V17, Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL, facade, null, "java/lang/Object", null)
            visitAnnotation("Lkotlin/Metadata;", true).apply {
                visit("k", metadata.kind)
                visit("mv", metadata.metadataVersion)
                visit("xi", metadata.extraInt)
                visitArray("d1").apply { metadata.data1.forEach { visit(null, it) } }.visitEnd()
                visitArray("d2").apply { metadata.data2.forEach { visit(null, it) } }.visitEnd()
            }.visitEnd()
            visitEnd()
        }.toByteArray()
        File(classes, "$facade.class").apply { parentFile.mkdirs() }.writeBytes(bytes)

        val names = indexClasspath(listOf(classes))

        assertTrue("androidx.compose.ui.platform.LocalContext" in names.callables, names.toString())
    }
}
