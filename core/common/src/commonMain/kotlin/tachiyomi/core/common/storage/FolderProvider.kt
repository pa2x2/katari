package tachiyomi.core.common.storage

import app.katari.core.common.platform.PlatformFacility
import java.io.File

@PlatformFacility
interface FolderProvider {

    fun directory(): File

    fun path(): String
}
