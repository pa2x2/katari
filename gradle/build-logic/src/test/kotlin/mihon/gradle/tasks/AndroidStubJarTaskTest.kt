package mihon.gradle.tasks

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AndroidStubJarTaskTest {

    @Test
    fun `the stub drops implemented classes with their nested ones and keeps classes that only share a name prefix`(
        @TempDir root: File,
    ) {
        root.resolve("java/android/os/Bundle.java").apply { parentFile.mkdirs() }.writeText("")
        root.resolve("kotlin/android/preference/PreferenceManager.kt").apply { parentFile.mkdirs() }.writeText("")
        val implemented = AndroidStubJarTask.implementedClassPaths(listOf(root.resolve("java"), root.resolve("kotlin")))

        val kept = listOf(
            "android/os/Bundle.class",
            "android/os/Bundle\$1.class",
            "android/os/BundleFoo.class",
            "android/preference/PreferenceManager\$OnActivityResultListener.class",
            "android/os/Parcel.class",
        ).filter { AndroidStubJarTask.isStubbed(it, implemented) }

        assertEquals(listOf("android/os/BundleFoo.class", "android/os/Parcel.class"), kept)
    }
}
