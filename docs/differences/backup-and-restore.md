# Backup and restore

Katari builds on Mihon's backup workflow while adding profiles, typed entries, type-specific state, and merged-entry data.

!!! info "Inherited behavior"

    Follow Mihon's [backup guide](https://mihon.app/docs/guides/backups) for general creation, automatic-backup, storage, and transfer advice. The differences below determine what Katari can preserve.

## Backup contents

The creation screen can include library entries, categories, chapters or episodes, tracking, history, non-library progress, app settings, extension stores, source settings, and optional private settings. The current interface retains the inherited **Manga** label for the library-entry option even though the backup supports typed entries.

A Katari profile-aware backup can preserve:

- Every profile's name, stable identity, order, archived state, and authentication requirement
- Profile categories, entries, progress, tracking, and history
- Profile-specific app and source preferences
- Entry type information
- Merged-entry relationships and playback-related Entry data
- The profile that was active when the backup was made

Global preferences and extension-store configuration are stored separately from profile bundles.

## Sensitive data

Private settings can include stored passwords, tokens, or other source credentials. Leave this option disabled unless it is required, and protect any backup that includes it. Keep copies outside the selected storage folder so device or storage failure does not remove both app data and backups.

## Restore behavior

When a Katari backup contains profiles, restore creates or updates them by stable profile identity, restores each profile's data in its own scope, then returns to the recorded active profile when possible. Existing data is merged according to the restore rules rather than replacing the entire database blindly.

A legacy Mihon or fork backup without Katari profiles is restored into the currently active profile. Supported legacy records are converted into typed Entry records.

Extensions and downloaded media are not stored in the backup. Restore reports missing sources and trackers; install the required extensions, sign in to trackers, and reindex existing downloads when necessary.

## Compatibility limits

Mihon can read its own shared backup data but does not understand Katari-only profile, additional entry-type, merged-entry, feed, or type-specific state. Do not rely on a round trip through Mihon to preserve Katari-specific state.

Before a major migration, keep the original backup and verify representative data in every profile.
