# Translation

Katari's Translation feature is designed to translate selected text across its different readers and other content
views.

## Availability

Katari currently uses the on-device translation service built into Android. Translation requires Android 12 or newer,
but support still depends on the device. The device manufacturer must provide a compatible translation service, and
that service must support the selected languages. Katari checks the selected profile engine, Android version, and
translation-service presence before enabling automatic reader translation. Language-pair and language-data readiness
are checked only after real text is selected.

## Automatic reader translation

Enable **Translate selected text automatically** from **More → Settings → Readers** or from a supported BOOK reader's
**General** settings tab. The profile-scoped setting is off by default. After it is enabled, selecting text and leaving
the selection settled briefly opens the existing translation popup. Moving the selection handles replaces an
in-progress request, so only the latest settled selection is translated.

Automatic selection translation is available in the serialized HTML prose reader and for reflowable EPUB. It is not
available for fixed-layout EPUB or readers that cannot provide stable selected text and an on-screen selection anchor.
EPUB keeps Readium's standard selection handles and Copy/Share actions; Katari does not add a separate **Translate**
selection-menu action.

If the selected engine or device translation service becomes unavailable, Katari preserves the enabled profile
preference but disables its effective behavior and explains why in reader settings. Translation resumes after
availability returns. Request-specific setup, source detection, target choices, and model states continue in the
translation session after a selection is made. Session content, including failures, stays anchored to a valid
selection when it fits safely; a missing or unusable anchor falls back to the adaptive sheet.

## Languages

Translation support is specific to each source and target language pair. A device may support two languages without
supporting translation between them.

The default target language follows Katari's app language until another language is selected. The source and target
languages can be changed for an individual translation without changing that default.

The Translation settings screen uses the playground as a staged profile editor. Changing its target language or
engine does not affect readers immediately; **Save** becomes available when either differs from the active profile
and persists both together. Leaving the screen without saving discards those changes. The playground's source
language and sample text remain request-only because reader translation detects the source language automatically.

Android manages the language files required for translation. Katari can show when setup is needed and open Android's
translation settings when the device provides them, but it does not download or delete language files itself.

## Profiles

The translation engine and default target language belong to the active [profile](./profiles.md). Switching profiles
can therefore change the translation defaults and whether automatic selected-text translation is enabled.

Text, results, and language changes made for one translation are temporary. They do not become profile defaults
unless the user explicitly saves them.

## Privacy

Katari does not keep the original text or translated result after the translation session ends.

Reader selections and translation UI state are also session-only. They are cleared when the selection disappears,
the reader navigates to another resource, automatic translation becomes ineffective, or the reader closes.

Katari has no translation history. Translation text and results are not stored in preferences or the database and are
not included in backups, telemetry, crash information, or logs.

Android owns the translation service, its language files, and any network access needed to install or update those
files. Copying a result places it on the Android clipboard, where Android's clipboard behavior applies.
