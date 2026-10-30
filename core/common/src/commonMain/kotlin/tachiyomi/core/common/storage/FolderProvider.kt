package tachiyomi.core.common.storage

import mihon.core.common.platform.PlatformFacility
import java.io.File

@PlatformFacility
interface FolderProvider {

    fun directory(): File

    fun path(): String
}
