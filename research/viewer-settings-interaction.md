# Viewer settings interaction plan

Status: milestone 1 implemented and awaiting review; changes are not committed.

This document preserves the decisions and implementation context for a shared settings interaction used by content readers and players. It is an internal research and implementation reference, not published documentation.

The immediate reason for this work is EPUB reader configuration. EPUB reader UI refinement is paused until this settings foundation exists. Manga will be migrated first and will serve as the reference implementation. Readium EPUB settings will be added in a later milestone.

## Problem

Reader and player settings are currently hard-wired:

- The original `SettingsReaderScreen` read manga `ReaderPreferences` directly.
- `SettingsPlayerScreen` reads `AnimePlayerPreferences` directly.
- Settings search indexes a static list of screens.
- BOOK has a registry of format-specific `BookProcessor` instances, but processors cannot contribute settings.
- Manga resolves its reading mode and orientation manually from a profile preference and per-entry bits in `Entry.viewerFlags`.

Adding Readium preferences directly to `SettingsReaderScreen` would provide a screen but would not provide the processor settings extension point required by the BOOK architecture.

## Accepted direction

Create a general viewer-settings interaction shared by reader and player providers. It must support:

- Manga reader settings.
- Anime player settings.
- One settings provider per configurable BOOK processor.
- Multiple processors for the same content format.
- Built-in processors now and observable discovery of installable reader extensions later.

Settings are owned by a processor/provider, not by an entry. A provider's profile defaults apply to every entry opened with that provider. Individual settings may additionally permit an explicit per-entry override.

Examples of stable provider identities:

- `builtin.manga.reader`
- `builtin.anime.player`
- `builtin.book.epub.readium`

Provider IDs and setting keys are persistence identities and must remain stable.

## Responsibility boundaries

The interaction discovers configurable providers and exposes them to the application settings UI.

Each provider owns:

- Setting definitions and stable keys.
- Processor defaults.
- Supported values and validation.
- Whether each setting may be overridden per entry.
- Applying effective values to its reader/player runtime.
- Migrations when the meaning or encoding of one of its values changes.

Katari owns:

- Active-profile scoping.
- Per-entry override persistence.
- Resolution of the effective value.
- Observable updates.
- Backup, restore, entry copying, migration, and deletion behavior.
- Provider discovery and duplicate-ID validation.
- Settings navigation, common rendering, search metadata, and accessibility boundaries.

The generic interaction must not understand EPUB concepts such as font size or Readium themes. A BOOK processor must not depend on the application's Voyager navigation, database implementation, or app-specific settings screen classes.

## Shared settings API

Fallback resolution must be implemented once in a shared API. Readers, players, standard controls, embedded custom controls, and custom screens must never reproduce logic such as:

```kotlin
entryOverride ?: profileValue ?: processorDefault
```

The shared API should expose a typed binding conceptually equivalent to:

```kotlin
interface ViewerSettingBinding<T> {
    val state: StateFlow<ResolvedViewerSetting<T>>

    suspend fun setProfileValue(value: T)
    suspend fun resetProfileValue()

    suspend fun setEntryOverride(value: T)
    suspend fun clearEntryOverride()
}

data class ResolvedViewerSetting<T>(
    val effectiveValue: T,
    val source: ViewerSettingSource,
    val processorDefault: T,
    val profileValue: T?,
    val entryOverride: T?,
)
```

Exact names may change during implementation, but the behavioral contract must not.

The resolver owns:

- The precedence chain.
- Validation at every layer.
- Reacting to active-profile changes.
- Reacting to profile preference changes.
- Reacting to entry override changes.
- Exposing the winning value and its source.
- All writes and reset operations.

The shared contracts should live in a small dedicated internal module, provisionally `entry-viewer-settings-api`. They must not be placed in `book-api`, because manga and anime use them too. The module is not part of the public reader-extension SDK yet, but it should avoid Readium, app, Voyager, and database types so it can become the basis of that SDK later.

## Value resolution and scopes

Effective values resolve in this order:

1. Valid per-entry override, when supported and present.
2. Valid active-profile value, when present.
3. Valid processor default.

Clearing an entry override deletes it. It must not copy the current profile value. This ensures future profile changes affect entries that inherit the default.

Initial setting scopes:

- `PROFILE_ONLY`
- `PROFILE_WITH_ENTRY_OVERRIDE`

Override policy is declared per setting, not once for a whole provider. Do not add `ENTRY_ONLY` initially. Stream, dub, subtitle, progress, bookmark, and similar entry/session state belongs to the corresponding state interaction rather than this configurable-defaults API.

Global Settings edits profile values. In a reader/player context:

- A profile-only control edits the profile value.
- An overrideable control edits the current entry override.
- An overrideable control exposes `Use default`, which clears the override and shows the resolved profile/processor default.

## Portable values and invalid data

Persist only bounded portable values, such as:

- Boolean.
- Integer.
- Decimal.
- String.
- Stable string choice identifiers.
- Color integer.
- String sets where justified.

Do not persist translated labels, choice indices whose order may change, or arbitrary Java/Kotlin serialized classes. Persist stable values such as `sepia`, not `2` or a localized label.

If an existing profile value or entry override becomes invalid after a provider update, the shared resolver should:

1. Ignore it when choosing the effective value.
2. Fall back to the next valid layer.
3. Preserve the raw value temporarily so the provider can migrate it.
4. Expose invalid-state information to Settings.
5. Allow reset to remove it.

An invalid processor default is a provider registration error and should reject the provider.

## Profile persistence

Settings are profile-scoped, matching existing manga reader and anime player settings. Changing the active profile must update bindings without recreating ad hoc preference objects throughout the reader.

New providers should use physically namespaced keys. Existing manga and anime profile preference key names should remain as compatibility-backed storage details. Migrating all existing raw key names provides no behavioral benefit and complicates upgrades and legacy backup restoration.

The shared API may logically address a setting as:

```text
builtin.manga.reader / reading_mode
```

while the profile storage adapter continues to use the established key:

```text
pref_default_reading_mode_key
```

This compatibility mapping must remain inside the provider/storage layer; consumers still use only the shared binding.

## Per-entry override persistence

Do not allocate more bits in `Entry.viewerFlags`, and do not store application settings in `Entry.memo`.

Use a dedicated generic table conceptually shaped as:

```text
entry_viewer_setting_overrides
├── entry_id
├── provider_id
├── setting_key
├── encoded_value
└── updated_at
```

The row identity is `entry_id + provider_id + setting_key`.

Required behavior:

- Entry deletion cascades to its overrides.
- Provider removal does not remove overrides.
- Reinstalling the same provider restores its overrides.
- Alternative processors have separate override namespaces.
- Unknown provider settings survive backup and restore.
- Entry copying, migration, and merge operations can preserve the opaque portable values.
- Values are size-bounded and validated before use.

The exact SQL representation may be refined during implementation, but it must support independent set/clear operations and observable changes without requiring processor-specific tables.

## Manga migration

Manga behavior and API usage will migrate completely as part of the first implementation work. Manga is not merely wrapped by a new Settings screen.

Required end state:

- All manga reader settings are registered through the viewer-settings provider.
- The Readers hub renders manga settings from that provider.
- The manga reader runtime consumes shared resolved bindings.
- The in-reader manga settings UI consumes the same bindings.
- Reading mode and orientation use generic per-entry overrides.
- Manga no longer resolves `entry ?? profile ?? default` itself.
- Manga stops writing reading mode and orientation into `Entry.viewerFlags`.
- Direct use of the old `ReaderPreferences` identity is removed; compatibility-backed storage is owned by `MangaReaderSettingsProvider`.

Perform a one-shot database migration:

- A non-default manga reading-mode flag becomes an override for `builtin.manga.reader / reading_mode`.
- A non-default manga orientation flag becomes an override for `builtin.manga.reader / orientation`.
- Default flag values create no rows because absence means inheritance.
- The migrated flag ranges cease to be sources of truth.

Legacy backup restoration must translate old `viewerFlags` into generic override rows. New backups must include generic overrides. A permanent dual-read or dual-write adapter is not acceptable because it creates two sources of truth.

Underlying profile preference key names remain unchanged for compatibility.

## Anime boundary

Anime participates in provider discovery and contributes its global player settings, such as picture-in-picture and seek preview.

Do not migrate the existing `playback_preferences` table wholesale. It contains entry-specific stream, dub, subtitle, quality, and subtitle-appearance selections. Many of these are playback selections/state rather than overrides of configurable profile defaults.

Individual anime settings may be deliberately moved into the shared inheritance model later, but that is separate work.

## Provider discovery

The interaction exposes an observable provider list even though the initial providers are built in. This preserves a path for reader extensions installed or removed while Katari is running.

Each configurable provider exposes at least:

- Stable ID.
- Display name and optional description.
- Category: reader or player.
- Built-in or extension origin metadata.
- Availability.
- Settings definitions.
- Reset behavior.
- Search metadata.

Registration rejects blank or duplicate IDs. Settings list all installed configurable providers even if the current library contains no compatible entry. Removing a provider preserves its stored values.

## Settings navigation

Use separate top-level hubs:

```text
Settings
├── Readers
│   ├── Manga reader
│   ├── EPUB reader — Readium
│   └── Future reader providers
└── Players
    ├── Anime player
    └── Future player providers
```

The existing Reader and Player destinations become provider hubs. Provider pages are one level deeper. Settings search should index provider settings and navigate directly to the relevant provider page, so the extra hierarchy does not add friction for search users.

Processor origin may be shown where useful, but technical format identifiers should not be the primary user-facing label.

## Settings control hierarchy

Support three presentation levels.

### Standard host controls

Katari renders common controls consistently:

- Switch.
- Choice.
- Slider or stepper.
- Text or numeric input.
- Color selection.
- Action/reset.
- Informational item.

### Embedded custom controls

A processor may contribute a custom control or section inside the host-owned settings scaffold. This is the normal escape hatch for richer configuration, such as:

- Theme swatches.
- Live font previews.
- Tap-zone diagrams.
- Subtitle-position previews.
- Margin or layout visualizations.

The host continues to own the screen scaffold, navigation, profile indication, search metadata, accessibility boundary, reset behavior, and settings bindings.

### Fully custom screens

A processor-owned full screen is reserved for genuinely complex configuration, such as an interactive gesture editor, multi-step engine setup, or layout calibration.

The eventual public reader-extension boundary must be versioned. It must not expose unrestricted Katari navigation or internal Voyager types. Embedded controls and custom screens receive bounded host capabilities and the same shared setting bindings; they cannot bypass resolution or persistence.

## Implementation sequence

Do not start until explicitly authorized by the user.

### Milestone 1: shared foundation and complete manga migration

1. Add the shared viewer-settings API and typed resolver.
2. Add portable value codecs and validation.
3. Add generic override SQL, repository, and observable queries.
4. Add database migration from manga `viewerFlags`.
5. Add legacy-backup conversion and new-backup persistence.
6. Add observable provider registration with duplicate validation.
7. Add Readers and Players hubs and dynamic settings search indexing.
8. Register the manga reader provider.
9. Migrate all manga settings UI and runtime consumers to shared bindings.
10. Remove manga reading-mode/orientation manual inheritance and `viewerFlags` writes.
11. Register the anime player provider for its existing global settings without migrating playback state.
12. Add standard controls and embedded custom-control hosting.
13. Stop for review without committing unless explicitly requested.

### Milestone 1 implementation result

The review candidate implements the approved foundation with these concrete identities:

- Internal contract module: `entry-viewer-settings-api`.
- Resolver and live binding implementation: `DefaultViewerSettingBinder`.
- Observable provider registry: `DefaultViewerSettingsInteraction`.
- Generic persistence table: `viewer_setting_overrides`.
- One-shot database migration: `36.sqm`, upgrading schema version 36 to 37.
- Manga provider: `builtin.manga.reader`, implemented by `MangaReaderSettingsProvider`.
- Anime provider: `builtin.anime.player`, implemented by `AnimePlayerPreferences`.
- Settings destinations: Readers and Players hubs, with separate Manga reader and Anime player pages.

The manga runtime and in-reader controls resolve reading mode and orientation through shared bindings. They no longer read or write those values through `Entry.viewerFlags`. The profile-level manga preferences keep their existing physical keys behind `MangaReaderSettingsProvider` for compatibility.

Generic overrides are included in backup/restore, entry migration, profile-scoped reset, and entry deletion through a foreign-key cascade. Legacy manga backup flags are translated during restore, while explicit generic backup rows take precedence. Unknown valid provider rows remain opaque and round-trip without requiring the provider to be installed.

The initial UI providers are built in. The registry supports observable registration and removal, while the eventual public reader-extension UI bridge remains deferred with the reader-extension SDK itself. Existing Katari preference controls provide the standard hosted controls and embedded widget boundary for the built-in provider pages; no Voyager or Compose type was added to the shared contract module.

### Milestone 2: Readium EPUB settings

After milestone 1 is reviewed:

1. Register `builtin.book.epub.readium` as a configurable reader provider.
2. Define EPUB processor defaults and per-setting override policies.
3. Bind Readium runtime preferences to the shared resolver.
4. Add standard and embedded appearance controls.
5. Validate that global Settings and future in-reader controls share live state.
6. Stop for review.

### Deferred EPUB reader refinement

Only after the settings foundation and Readium settings are reviewed, return to reader chrome and navigation:

- Tap-to-toggle controls.
- Top and bottom bars.
- Section page information and publication progress.
- Table of contents.
- Position navigation.
- Search, bookmarks, highlights, and annotations in later milestones.

## Validation expectations

At minimum, cover:

- Processor default resolution.
- Profile value precedence.
- Entry override precedence.
- Clearing an override restores inheritance.
- Active-profile switching updates bindings.
- Invalid profile and override values fall back correctly.
- Profile reset and entry reset are independent.
- Provider ID and setting key collision rejection.
- Provider removal and reinstall preserve settings.
- Manga `viewerFlags` migration for every reading mode and orientation.
- Legacy backup conversion.
- New backup/restore round trip for unknown provider overrides.
- Entry deletion cascade.
- Entry copy/migration behavior.
- Standard and custom controls receive the same binding state.
- Settings search discovers provider-owned controls.

Run repository-required formatting and focused unit tests. Because this work changes SQLDelight schema and migrations, run `verifySqlDelightMigration`. Follow with focused FOSS compilation and broader release-shaped validation in proportion to the final change set. Do not use an emulator or physical device unless explicitly authorized.

## Explicit non-goals

- Do not refine EPUB reader chrome in milestone 1.
- Do not add per-entry EPUB appearance decisions before the provider settings are defined.
- Do not publish a reader-extension SDK yet.
- Do not put viewer settings contracts in `book-api`.
- Do not expose Readium types across the generic interaction.
- Do not reuse `Entry.viewerFlags` for new processor settings.
- Do not use `Entry.memo` as override storage.
- Do not migrate anime playback state wholesale.
- Do not implement DRM handling.
- Do not include audiobook behavior in BOOK.
