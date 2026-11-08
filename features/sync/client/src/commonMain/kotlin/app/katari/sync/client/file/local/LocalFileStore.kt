package app.katari.sync.client.file.local

import java.io.File

/**
 * Where the app keeps the files sync carries, as it keeps them for itself: custom covers by local entry ID, imported
 * book reader fonts by profile and file name. The app provides it.
 *
 * Every change to these files, whoever makes it, is reported to [watch]'s listeners, sync's own writes through [put]
 * and [remove] included: that is how sync hears the person's changes, from any screen, and it tells its own apart
 * by their content.
 */
interface LocalFileStore {
    /** The owner's file, or null when it has none. */
    fun file(owner: FileOwner): File?

    /** Replaces the owner's file with a copy of [source] in one step, so nothing ever reads half a file. */
    fun put(owner: FileOwner, source: File)

    fun remove(owner: FileOwner)

    /** The file names of the fonts the profile imported. */
    fun fonts(profileId: Long): List<String>

    /** Calls [onChange] after each change to an owner's file, on the thread that made it, until closed. */
    fun watch(onChange: (FileOwner) -> Unit): AutoCloseable
}
