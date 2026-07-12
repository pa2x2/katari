# Profiles

Profiles provide separate personal spaces inside one Katari installation. When more than one visible profile exists, switch profiles through **More → Switch profile** or through the optional **Profiles** home tab when it is enabled. Manage them under **More → Settings → Profiles**.

## What is profile-specific

Each profile has its own:

- Library entries, categories, updates, and history
- Library display and update behavior
- Source visibility and source preferences
- Reader, player, appearance, and security preferences assigned to profiles
- Tracking logins and entry tracking state
- Startup-screen preference
- Feeds, merged entries, and playback progress associated with its entries

Settings marked with a **Profile-specific** chip apply only to the active profile.

## What remains global

Downloads, backups, networking, privacy, storage, extension installation/repository management, and other installation-wide controls are shared. A global change affects the whole app even when made while one profile is active.

## Create and switch profiles

In **Settings → Profiles → Manage profiles**, use the add action and enter a unique, non-empty name. Enable **Choose profile on launch** if the profile picker should appear when Katari starts and more than one visible profile is available.

Switching profiles changes the active library and profile-scoped settings immediately. Background library work is rescheduled for the selected profile.

## Archive and delete

Archiving hides a non-default, inactive profile from the normal picker without deleting its data. Archived profiles can be restored or permanently deleted. The default profile cannot be archived, and the active profile must be switched before it can be archived.

Permanent deletion removes that profile's database and preference state. Create a backup first if the data may be needed later.

## Move library entries

Select library entries and choose **Move to profile**. Pick the destination profile and category. If the destination already contains the same source, URL, and type, choose **Don't transfer** to keep the source group, **Overwrite destination**, or **Remove from current profile** to keep the destination and remove the source copy. Entries moved successfully are removed from the source profile as part of the move.

A locked destination requires authentication. Selecting any member of a merged group moves the complete ordered group and recreates it in the destination profile. Resolving destination conflicts can detach or remove conflicting destination members, depending on the selected conflict action.

## Backups

Katari backups retain profile identity, ordering, archived state, profile-scoped preferences, categories, and entries. See [Backup and restore](../differences/backup-and-restore.md).
