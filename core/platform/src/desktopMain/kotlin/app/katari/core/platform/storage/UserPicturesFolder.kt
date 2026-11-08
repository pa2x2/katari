package app.katari.core.platform.storage

import app.katari.core.common.platform.PlatformFacility
import java.nio.file.Path

/** The user's own pictures folder on the desktop, where saved images go, as Android saves them to Pictures. */
@PlatformFacility
fun interface UserPicturesFolder {
    /** The folder, or null where the desktop names none. */
    fun path(): Path?
}
