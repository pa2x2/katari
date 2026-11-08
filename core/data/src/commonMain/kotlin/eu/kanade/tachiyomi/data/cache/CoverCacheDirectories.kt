package eu.kanade.tachiyomi.data.cache

import mihon.core.common.platform.PlatformFacility
import java.io.File

/** Where [CoverCache] keeps its files on the platform it runs on. Each directory exists by the time its getter returns. */
@PlatformFacility
interface CoverCacheDirectories {
    /** Library entries' covers downloaded from their thumbnail URLs, which can be downloaded again. */
    val covers: File

    /** The covers users set themselves, which exist nowhere else. */
    val customCovers: File
}
