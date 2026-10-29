package mihon.core.platform.storage

import java.io.File

/** The directories only the app itself sees, as the platform it runs on keeps them. Both exist when read. */
interface AppPrivateDirectories {
    /** Files the app can do without, which the system or the user may clear at any time. */
    val cache: File

    /** Files the app keeps until it deletes them itself. */
    val files: File
}
