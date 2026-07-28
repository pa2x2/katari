# Translation

Katari's Translation feature is designed to translate selected text across its different readers and other content
views.

## Availability

Katari currently uses the on-device translation service built into Android. Translation requires Android 12 or newer,
but support still depends on the device. The device manufacturer must provide a compatible translation service, and
that service must support the selected languages.

## Languages

Translation support is specific to each source and target language pair. A device may support two languages without
supporting translation between them.

The default target language follows Katari's app language until another language is selected. The source and target
languages can be changed for an individual translation without changing that default.

Android manages the language files required for translation. Katari can show when setup is needed and open Android's
translation settings when the device provides them, but it does not download or delete language files itself.

## Profiles

The translation engine and default target language belong to the active [profile](./profiles.md). Switching profiles
can therefore change the translation defaults.

Text, results, and language changes made for one translation are temporary. They do not become profile defaults
unless the user explicitly saves them.

## Privacy

Katari does not keep the original text or translated result after the translation session ends.

Katari has no translation history. Translation text and results are not stored in preferences or the database and are
not included in backups, telemetry, crash information, or logs.

Android owns the translation service, its language files, and any network access needed to install or update those
files. Copying a result places it on the Android clipboard, where Android's clipboard behavior applies.
