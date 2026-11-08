package app.katari.profiles.core

import app.katari.entry.interactions.lifecycle.removal.EntryDestructiveRemovalFeature
import app.katari.entry.interactions.lifecycle.removal.EntryDestructiveRemovalResult
import app.katari.entry.interactions.reader.settings.BookDocumentReaderFontLibrary
import app.katari.sync.client.kind.profile.LocalProfileDeletion
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.library.service.LibraryUpdateQueue
import tachiyomi.domain.library.service.LibraryUpdateSchedule

/**
 * Deletes a profile with everything it owns: its entries with their downloads and custom covers, its library update
 * work, its row, its preferences, its extension settings and its reader fonts. The person deletes a profile through
 * [ProfileManager], which decides when a profile may go; sync deletes one that was deleted on another device.
 *
 * The entries go first, so when their removal fails the profile stays whole and the deletion can be tried again.
 */
@Inject
@ContributesBinding(AppScope::class)
class ProfileDeletion(
    private val profileDatabase: ProfileDatabase,
    private val profileStore: ProfileStore,
    private val extensions: ProfileExtensions,
    private val entryRepository: EntryRepository,
    private val destructiveRemoval: EntryDestructiveRemovalFeature,
    private val bookReaderFonts: BookDocumentReaderFontLibrary,
    private val libraryUpdateQueue: LibraryUpdateQueue,
    private val libraryUpdateSchedule: LibraryUpdateSchedule,
) : LocalProfileDeletion {
    override suspend fun delete(profileId: Long) {
        when (val result = destructiveRemoval.remove(entryRepository.getAllEntriesByProfile(profileId))) {
            is EntryDestructiveRemovalResult.Failed -> throw result.cause
            EntryDestructiveRemovalResult.NoChange -> Unit
            is EntryDestructiveRemovalResult.Removed -> {
                result.failures.forEach { failure ->
                    logcat(LogPriority.ERROR, failure.cause) {
                        "Profile deletion consequence ${failure.consequence} failed"
                    }
                }
            }
        }
        libraryUpdateQueue.stop(profileId)
        libraryUpdateSchedule.cancel(profileId)
        profileDatabase.deleteProfile(profileId)
        profileStore.deleteProfileState(profileId)
        extensions.deleteSettings(profileId)
        bookReaderFonts.deleteProfile(profileId)
    }
}
