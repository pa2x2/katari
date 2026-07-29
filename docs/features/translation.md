# Translation

Katari translates selected text through an explicitly selected translation
engine. Translation engines are peers: Katari does not silently fall back to a
different engine when the selected one is missing, unconfigured, or temporarily
unavailable. A profile without an explicit choice uses Android System
Translation as an implicit default only while that service is available on the
device. Otherwise, the profile remains unconfigured until you choose an engine.

## Engines

Choose an engine from **More → Settings → Translation → Translation engine**.
The list includes supported provider apps even when they are not installed.
Each engine card keeps its selection, readiness, explanation, recovery action,
and details together. Unavailable cards remain readable but cannot be selected;
use their explicit **Install**, **Configure**, or **Manage** action to recover.
Those actions never select the engine automatically. An explicitly selected
engine remains selected if it later becomes unavailable. An unavailable
implicit Android default is not shown as selected because the profile has not
configured an engine.

Choose **Details** on any card to see where that engine processes text, its
privacy behavior, current status, artwork attribution, and provider
documentation without leaving the engine list. The compact chooser shown during
a translation uses the same statuses and recovery actions.

### Android System Translation

Android System Translation is the implicit default for a profile without an
explicit engine choice. It requires Android 12 or newer and a compatible
translation service supplied by the operating system or device manufacturer.
The implicit default becomes effective only when that service is available. A
device without it, including a GrapheneOS installation without a compatible
provider, reports Android System Translation as unavailable, leaves every
engine unselected, and keeps the other engines visible.

Android owns this engine's language files. Katari can show when setup is needed
and open Android's translation settings when the device provides them, but it
does not download or delete Android language files.

The Android robot is reproduced or modified from work created and shared by
Google and used according to terms described in the
[Creative Commons 3.0 Attribution License](https://creativecommons.org/licenses/by/3.0/).
Android is a trademark of Google LLC. See the
[Android brand guidelines](https://developer.android.com/distribute/marketing-tools/brand-guidelines).

### Offline Translator

[Offline Translator](https://f-droid.org/packages/dev.davidv.translator/) is a
separate app that runs LibreTranslate-compatible models on the device. Its row
remains visible when the app is missing; choose **Install** to open its F-Droid
page.

After installing it:

1. Open Offline Translator and download the required language models.
2. Enable its HTTP API and keep the API bound to localhost.
3. In Katari, choose **Configure**, enter the API port, and test the connection.
   The default port is `5000`; valid ports are `1` through `65535`.
4. Choose **Done** after Katari reports **Ready to use**, then select Offline
   Translator from the engine list.

Katari connects only to `127.0.0.1` for this engine and bypasses configured
network proxies. The first translation asks for confirmation that selected text
will cross the app boundary over localhost. Katari discovers actual language
and pair support from Offline Translator's `/languages` endpoint and sends
translations to its `/translate` endpoint.

### LibreTranslate Server

The LibreTranslate Server engine connects to an existing
[LibreTranslate](https://docs.libretranslate.com/) deployment and returns the
translation inline in Katari. Choose **Configure** to enter its base URL and an
optional API key, then choose **Save and test**. Setup reports progress and
failure inline; choose **Done** after it reports **Ready to use**. The engine
cannot be selected until that test succeeds.

Remote endpoints must use HTTPS. Cleartext HTTP is accepted only for
`localhost`, `127.0.0.1`, or `::1`. Katari rejects endpoint URLs containing
embedded credentials, a query, or a fragment. Optional API keys are encrypted
at rest with a provider-scoped key held by Android Keystore and are sent only in
the translation request body.

A later server outage makes the engine temporarily unavailable but does not
erase the profile's selection or server configuration. Katari does not switch
to Android System Translation or another engine.

## Automatic reader translation

Enable **Translate selected text automatically** from either capable BOOK
reader under **More → Settings → Readers**, or from a supported BOOK reader's
**General** settings tab. The profile-scoped setting is off by default and
stored independently for each reader. Changing a reader-specific or in-reader
toggle affects only that reader. After it is enabled, selecting text and
leaving the selection settled briefly opens the existing translation popup.
Moving the selection handles replaces an in-progress request, so only the
latest settled selection is translated.

Automatic selection translation is available in the serialized HTML prose
reader and for reflowable EPUB. It is not available for fixed-layout EPUB or
readers that cannot provide stable selected text and an on-screen selection
anchor. EPUB keeps Readium's standard selection handles and Copy/Share actions;
Katari does not add a separate **Translate** selection-menu action.

If an explicitly selected engine becomes unavailable, Katari preserves the
enabled profile preference but disables its effective behavior and explains why
in reader settings. Translation resumes when that same engine becomes
available. If the implicit Android default is unavailable, reader settings
instead ask you to choose an engine.
Request-specific setup, source detection, target choices, and language-data
states continue in the translation session after a selection is made. Session
content, including failures, stays anchored to a valid selection when it fits
safely; a missing or unusable anchor falls back to the adaptive sheet.

## Languages

Translation support is specific to each engine and each source/target language
pair. Katari queries provider capabilities rather than assuming that support
for two languages means the pair is supported.

Language pickers show capabilities reported by the selected engine. When an
engine reports exact source/target pairs, the source picker shows its supported
source languages and the target picker shows targets compatible with the
staged or detected source. Engines that can report languages but not exact
pairs filter source and target choices independently. The full device language
list is used only when an engine explicitly declares that it accepts arbitrary
language tags.

If an engine's language capabilities cannot be loaded, its language picker
stays unavailable and offers a retry instead of falling back to unrelated
device languages. Switching engines never silently rewrites the staged source
or target. An unsupported staged pair remains visible until a supported pair
is chosen, and cannot be saved as the profile default.

The default target language follows Katari's app language until another
language is selected. Source and target languages can be changed for an
individual translation without changing that default.

The Translation settings screen uses the playground as a staged profile editor.
Changing its target language or engine does not affect readers immediately;
**Save** becomes available when either differs from the active profile and
persists both together. Leaving the screen without saving discards those
changes. The playground's source language and sample text remain request-only
because reader translation detects the source language automatically.

Language data is owned by the selected provider. Android manages Android System
Translation data, Offline Translator manages its downloaded models, and a
LibreTranslate server operator manages the server's language catalog.

## Profiles

The explicit translation engine choice and default target language belong to
the active [profile](./profiles.md). Switching profiles can therefore change
the translation defaults and whether automatic selected-text translation is
enabled. Profiles without an explicit engine choice independently evaluate the
availability-dependent Android default.

Text, results, and language changes made for one translation are temporary.
They do not become profile defaults unless the user explicitly saves them.

## Privacy

Katari does not keep the original text or translated result after the
translation session ends.

Reader selections and translation UI state are also session-only. They are
cleared when the selection disappears, the reader navigates to another
resource, automatic translation becomes ineffective, or the reader closes.

Katari has no translation history. Translation text and results are not stored
in preferences or the database and are not included in backups, telemetry,
crash information, or logs.

The selected engine determines where text is processed:

- Android System Translation passes text to Android's translation service.
- Offline Translator passes text to a separate app over localhost and processes
  it with that app's downloaded models.
- LibreTranslate Server sends text to the configured server; that server
  operator's privacy and retention policies apply.

Katari displays the relevant disclosure before the first translation with an
external provider. Copying a result places it on the Android clipboard, where
Android's clipboard behavior applies.
