# Migrating to Katari

Katari can restore a Mihon backup and reuse a Mihon storage folder. Migration copies supported data; it does not modify or remove the Mihon installation.

::: info Inherited foundation

Mihon's [backup guide](https://mihon.app/docs/guides/backups) explains how backups work generally. This page covers the Katari migration path and the differences that matter when moving from Mihon.

:::

## Before you begin

1. Update Mihon and create a fresh backup in **More → Data and storage → Create backup**.
2. Select everything you want to retain. Include private settings only if you intend to transfer stored source preferences and sign-in details; backup files can then contain sensitive data.
3. Keep a separate copy of the backup until you have verified the Katari library.
4. Do not uninstall Mihon or delete its storage yet.

Extensions and downloaded files are not embedded in a Mihon backup.

## Restore during Katari setup

When Katari detects an installed Mihon, onboarding displays **Move from Mihon**:

1. Use **Open Mihon to create a backup** if you still need a current backup.
2. Return to Katari and choose **Choose Mihon backup**.
3. Review the restore contents and begin the restore.
4. Install any reported missing extensions and sign in to any reported tracking services.

If the migration prompt is not shown, finish onboarding and use **More → Data and storage → Restore backup**.

## Reuse downloads

Select the same storage folder used by Mihon during Katari setup. Shared extensions remain available to Katari when Android exposes the installed package, but privately installed extensions must be installed again.

If existing downloads do not appear, confirm the storage location and use **More → Settings → Advanced → Reindex downloads**. Do not move or rename source and title folders until both apps have finished using them.

## How Mihon data enters Katari

A Mihon backup without Katari profiles is restored into the active Katari profile. Restored content is converted to Katari entries of the corresponding type. Katari then stores library, category, history, tracking, and source data within that profile.

Mihon cannot provide Katari-only state such as:

- Additional profiles
- Playback progress and preferences for media types unavailable in Mihon
- Merged-entry groups
- Katari feed configuration
- Entry-native extension state unavailable in Mihon

## Verify before removing Mihon

Check representative library entries, categories, read progress, history, trackers, source preferences, and downloads. Keep the original backup and Mihon installation until those checks pass.

Katari and Mihon use different application identities. They can coexist, but automatic jobs and both apps accessing the same downloads may be confusing. Once migration is verified, decide which app should continue managing that storage.
