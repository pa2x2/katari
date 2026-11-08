package app.katari.sync.client.kind.profile

/**
 * Deletes a profile on this device with everything it owns, exactly as the person deleting it here does, files
 * included. The app provides it; sync calls it inside its own transaction, so the profile's rows go with the records
 * that deleted it and none of the deletion is captured as an edit of this device's.
 */
interface LocalProfileDeletion {
    suspend fun delete(profileId: Long)
}
