---
name: website-update
description: Bring the Katari website's pages up to date with app and extension SDK changes. Use when asked to update or review the website after a release or a merged change, or when the release-notes skill hands off to it.
---

## What already updates itself

Leave these alone; they follow the release without edits here:

- The home page's What's new card reads `CHANGELOG.md` when the site builds.
- `showcase.yml` recaptures every still and clip from a published release and proposes the changed ones against the
  `assets` branch. Never edit `website/src/captures` by hand.
- `website-release.yml` redeploys the site when a release is published.

What drifts is the prose: the home page and the pages under `website/src/pages`, which name the app's labels and
describe what it does.

## Collect the changes

1. When the release-notes skill hands off, take its new `CHANGELOG.md` and `SDK-CHANGELOG.md` outcomes as the list
   to check. Otherwise derive outcomes from `git log main..HEAD` and `git diff main...HEAD`, the same way release-notes
   does, keeping only those a user or extension developer would notice.
2. Inspect `git diff main...HEAD -- i18n/src/commonMain/moko-resources/base/strings.xml`. Every changed or removed
   string is a label the site may still quote.
3. Create a temporary file to track each outcome and its verdict.

## Find stale pages

1. Search `website/src` for every changed or removed label's old text, and for the screens, settings and behaviour each
   changed or removed outcome touches. App outcomes map to the home page and `guide/`; changes to the modules
   published by `jitpack.yml` map to `guide/extensions/`.
2. Check each match against the current source: labels inside `<Path>` and `<strong>` must match the base
   `strings.xml` exactly, including the order of screens in a path, and every claim about behaviour must hold in the
   code that implements it.
3. Also correct anything wrong you come across on a page you are already checking, even when no change in the range
   caused it.

## Cover new outcomes

1. For each added outcome, decide whether it belongs in a section of an existing page, earns a page of its own, or
   needs nothing. Most fixes and small improvements need nothing; the site explains features, it does not repeat the
   changelog.
2. A new page follows the existing pages' components (`GuidePage`, `Task`, `Path`, `Callout`) and is registered in
   `website/src/guide/contents.ts`, with its icon copied from the symbol the app shows for the feature.
3. When a page needs a still or clip that does not exist, do not make one by hand. Report the scene it needs so it can
   be added to `showcase/capture` and `showcase/pipeline/scenes.ts`.

## Writing rules

1. Name things with the app's own labels and describe what the reader sees and does. Leave out implementation details,
   class names and internal terms; extension pages may name the SDK's public types.
2. Use the `unslop` skill to write the text. If it is unavailable, keep sentences plain and factual rather than
   promotional.
3. Match the surrounding page's length and tone; do not grow a section beyond what the change needs.

## Verify

1. Run `npm run build` in `website/` and fix every error and warning it reports.
2. Serve the build and screenshot each changed section at 1440, 1100, 820 and 390 px. Read every image, and confirm no
   page scrolls sideways (`scrollWidth` against `clientWidth`) at any width.
3. Do not commit. Report the pages changed, the outcomes reviewed that needed no change, and any captures the
   showcase still needs.
