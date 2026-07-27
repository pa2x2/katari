# Prose reader content support plan

Status: **Implementation complete**

This plan expands the built-in `prose-chapter` reader from its current text-span subset into a structured, passive document reader that preserves the meaningful content commonly found in English web novels.

The plan is intentionally implementation-free. Its phases describe the required product behavior, architecture boundaries, and validation criteria; completing this document does not authorize production or test changes.

## Scope

### Included

The following capabilities are in scope, in product-priority order:

1. Thematic and scene breaks.
2. Ordered lists, including numbering metadata.
3. Preformatted prose, code, and system-message blocks.
4. Images, figures, captions, and alternative text.
5. Tables and structured status panels.
6. Intentional whitespace and basic alignment.
7. Footnotes and endnotes.
8. Safe external links.
9. Collapsible and spoiler sections.
10. Safe presentation styling: colors, borders, backgrounds, and custom fonts.

Source normalization must also be aligned with the reader contract so extensions do not discard supported content before the processor validates it.

### Explicitly excluded

- Ruby annotations such as `<ruby>` and `<rt>`.
- Language and per-block direction metadata.
- SVG, mathematical notation, and specialized chart rendering.
- Scripts, event handlers, forms, iframes, canvas, executable widgets, and arbitrary website chrome.
- Audio and video as prose document blocks.
- Unrestricted CSS execution or unrestricted remote font loading.

An excluded content-bearing block must become the explicit unsupported-content fallback defined below. It must never execute provider-controlled code.

### Unsupported content visibility

The normalized document model must include a first-class unsupported block. When a chapter contains a content-bearing block that the prose contract does not support, the reader must render this exact visible text in its place:

```text
-- unsupported content block --
```

Examples include unsupported audio, video, embedded objects, SVG, mathematical blocks, canvas content, or another block-level format for which Katari has no safe renderer.

The placeholder exists so unsupported content is observable during ordinary reading and can be investigated for future support. The normalized block should retain a safe internal diagnostic reason or original element type, but the reader-facing fallback remains the exact text above. Diagnostic metadata must not retain credentials, executable markup, unsafe URLs, or unbounded provider content.

Fallback rules:

- emit one placeholder for the outermost unsupported content block rather than one for every nested child such as `<source>` or `<track>`;
- do not make network requests, instantiate media components, or execute fallback markup for an unsupported block;
- include unsupported blocks in logical document extent, locators, progress, accessibility, continuous mode, paginated mode, search, and offline packages;
- preserve the placeholder when the original subordinate resource is unavailable, malformed, or rejected by security validation;
- harmless unknown inline wrappers should retain their safe readable text instead of replacing a sentence with a block placeholder;
- stripped scripts, event handlers, stylesheets, tracking metadata, comments, forms, advertisements, navigation chrome, and other non-chapter active material do not produce placeholders;
- an unsupported block nested inside supported prose produces a placeholder at the corresponding document position without discarding the surrounding supported content.

## Product requirements

All included content must behave consistently in continuous and paginated reading modes. Supporting a construct in only one mode is incomplete.

The implementation must preserve:

- stable chapter and block locators;
- progression across loading, reflow, disclosure expansion, and reader-mode changes;
- adjacent-chapter preloading and transitions;
- tap zones and reader chrome behavior;
- accessibility descriptions and readable fallbacks;
- offline chapter completeness;
- source-request headers needed for authorized assets;
- bounded memory, disk, network, and decoded-image usage;
- safe failure behavior for unavailable or malformed subordinate resources.

The reader must render content from a structured document model. Admitting more tags into a sanitizer while still flattening everything through `HtmlCompat` is not sufficient.

## Architecture boundaries

### Source and processor responsibilities

Sources provide the selected chapter body and stable references to subordinate resources. They may remove provider website chrome, but they must preserve every construct and attribute supported by the prose contract.

The app-side processor remains the final trust boundary. It must independently validate tags, attributes, URLs, styles, asset sizes, and resource relationships even when an extension already normalized the response.

The source contract must not expose executable callbacks to the reader. Assets must resolve through Katari-owned resource access, networking, caching, and download paths.

### Structured document model

Evolve the internal BOOK document representation so each supported construct has an explicit semantic form rather than being inferred from a rendered `Spanned` value. The target model needs, at minimum:

- text and heading blocks;
- thematic-break blocks;
- ordered and unordered list structures with list items;
- preformatted/system blocks;
- image/figure blocks with caption and alternative text;
- table structures with rows, cells, headers, and spans;
- aligned or whitespace-preserving text blocks;
- note references and note bodies;
- external-link annotations;
- disclosure blocks with summary and expanded content;
- safe style tokens and font references.
- unsupported content blocks with bounded diagnostic metadata.

Logical text extent and resource progression must remain deterministic for non-text blocks. Locators must not depend on transient pixel height, network completion order, or whether a disclosure is currently expanded.

Keep the model internal until its behavior is validated by another structured BOOK processor or a deliberate public-contract decision is made.

### Rendering

Replace the assumption that every block is one styled `TextView` with block-specific rendering. Shared reader typography and user settings remain authoritative over provider defaults unless a scoped content style carries meaning.

Block renderers must provide stable placeholders before asynchronous assets resolve so content does not jump unpredictably. Paginated mode must repaginate deterministically when an included block changes its measured extent and retain the closest logical locator.

### Styling security

Do not pass arbitrary provider CSS into Android views or a WebView.

Translate a reviewed subset into typed values:

- text and background colors with contrast correction;
- border style, width, and color within fixed limits;
- block emphasis/callout roles;
- start, center, and end alignment;
- preserved whitespace modes;
- bounded font size and weight;
- approved generic font families;
- validated packaged or remote font resources when custom fonts are required.

Remote fonts follow the same controlled asset policy as images: HTTPS-only retrieval, bounded size, validated media type, cache identity, offline packaging, and safe fallback. Reader-selected fonts may override decorative provider fonts, but meaning-bearing fonts must have a readable fallback.

## Implementation phases

### Phase 0: Characterize and lock the contract

- Create a prose capability matrix covering accepted input, semantic model output, continuous rendering, paginated rendering, offline behavior, and fallback behavior.
- Inventory current `prose-chapter` producers and the HTML emitted by each provider.
- Compare every producer allowlist with the processor contract.
- Classify provider elements as supported content, unsupported content-bearing blocks, harmless inline wrappers, or removable non-chapter/active material.
- Define safe URL schemes, media types, size limits, redirect rules, header handling, cache identity, and failure policy for subordinate assets.
- Define logical progression rules for non-text and expandable blocks before changing the model.
- Record supported tags and attributes in the extension-facing prose documentation.

Exit criteria:

- Every included capability has an agreed normalized representation and fallback.
- Every unsupported content-bearing block maps deterministically to one visible unsupported block.
- Security and offline behavior are specified before remote resources are admitted.
- Existing supported prose behavior has focused regression coverage at its owning boundary.

### Phase 1: Correct text and structural fidelity

Implement the high-frequency constructs that do not require remote assets:

- render thematic breaks as dedicated blocks;
- preserve ordered-list numbering, `start`, nesting, and item boundaries;
- render preformatted and code/system blocks with preserved whitespace and appropriate typography;
- preserve meaningful indentation and start/center/end alignment;
- keep stable block IDs and anchor offsets across the new structures.

Exit criteria:

- Scene boundaries never disappear.
- Ordered lists retain their authored numbering in both modes.
- Whitespace-sensitive passages retain line breaks and indentation within reader-width constraints.
- Mode switches and font/layout changes retain the closest logical reading position.

### Phase 2: Structured tables and status panels

- Parse tables into rows, cells, headers, captions, `colspan`, and `rowspan`.
- Render simple tables accessibly without flattening cell boundaries.
- Provide bounded horizontal scrolling or a reviewed narrow-screen reflow policy.
- Introduce a typed status/callout presentation for common web-novel system panels without depending on provider CSS classes.
- Define graceful fallback for malformed or excessively complex tables.

Exit criteria:

- Cell order and relationships remain understandable on narrow and wide screens.
- Screen readers receive useful row/header context.
- Oversized tables cannot force unbounded measurement or memory use.
- Pagination does not split content into an unusable or non-navigable state.

### Phase 3: Images, figures, and asset delivery

- Add image and figure nodes with source identity, alternative text, caption, intrinsic dimensions when known, and presentation hints.
- Resolve remote resources through Katari-owned networking with required source headers and redirect validation.
- Validate URL scheme, content type, encoded size, decoded dimensions, and memory cost.
- Add stable loading, error, and alt-text fallbacks.
- Cache by stable content identity rather than expiring acquisition URL.
- Package required image and font assets with offline chapters and verify the package before publishing a completed download.
- Support reader-width scaling, aspect-ratio preservation, zoom/open behavior if approved, and theme-safe transparent images.

Exit criteria:

- Images cannot make unaudited network requests from a `TextView` or WebView.
- Missing or rejected images leave readable alternative content.
- Loading does not lose the current locator or cause uncontrolled layout jumps.
- Offline chapters never claim completeness while required supported assets are absent.

### Phase 4: References and disclosure content

- Model footnote/endnote references and bodies separately from ordinary links.
- Preserve same-document navigation and add return-to-reference behavior.
- Choose and implement a consistent note presentation for both reading modes.
- Admit external HTTP/HTTPS links only after sanitization; open them through an explicit user action and the shared browser capability rather than inline navigation.
- Model `<details>`/`<summary>`-like content as native disclosure blocks.
- Preserve disclosure content in search, accessibility, and logical progression regardless of expansion state.

Exit criteria:

- Notes are reachable and returnable without losing reading position.
- External links cannot launch unsupported schemes or silently navigate the reader.
- Disclosure state changes do not corrupt progress or make content permanently unreachable.

### Phase 5: Safe presentation styling and custom fonts

- Map the reviewed safe style subset into typed document style values.
- Apply contrast correction against every prose reader theme.
- Bound borders, padding, font sizes, and other layout-affecting values.
- Resolve permitted custom font assets through the controlled asset pipeline.
- Define precedence between author styles, semantic block styles, and user reader settings.
- Provide deterministic fallback when a style or font is rejected or unavailable.

Exit criteria:

- Meaning-bearing callouts remain distinguishable in every theme.
- Provider styling cannot obscure text, controls, links, or accessibility focus.
- Custom fonts cannot bypass resource limits or break offline completeness.
- User accessibility and reader preferences retain documented precedence.

### Phase 6: Producer alignment

After the app processor supports the relevant structures:

- update NovelBuddy normalization to preserve the supported prose subset and required safe attributes;
- apply the same contract to every other `prose-chapter` producer;
- avoid source-specific rendering semantics when a provider-neutral representation exists;
- keep provider website chrome and active content excluded;
- verify real provider samples for every construct that a source claims to preserve.

Exit criteria:

- No producer strips a construct that the matching runtime supports.
- No producer labels unsupported active content as supported passive prose.
- Provider-specific request headers and asset identities survive normalization without leaking credentials.

### Phase 7: Validation and documentation

Add only tests that protect durable behavior or a real boundary:

- parser/normalizer contract tests for each semantic block;
- renderer tests for continuous and paginated behavior;
- locator and progression tests around non-text, expandable, and reflowing blocks;
- network and asset security tests;
- download completeness and cache-identity tests;
- accessibility and fallback tests;
- unsupported-block classification tests that distinguish content-bearing blocks from removable scripts, tracking, and website chrome;
- renderer and locator tests for the exact `-- unsupported content block --` fallback in both reading modes;
- focused extension validation against real provider responses where applicable.

Run the repository-required formatting and focused BOOK checks during implementation. Broader architecture, ABI, migration, unit, and release validation should be selected according to the files actually touched and run in the repository-prescribed separate invocations.

Update:

- extension prose media documentation;
- user-facing BOOK/prose reader capability documentation;
- SDK changelog or versioning notes if a public source contract changes;
- source-specific documentation only when behavior genuinely differs.

## Completion criteria

The plan is complete only when:

- all included constructs have first-class normalized representations;
- continuous and paginated modes preserve their meaning;
- online and offline behavior are honest and complete;
- unsupported or rejected content has a safe readable fallback;
- unsupported content-bearing blocks render the exact visible `-- unsupported content block --` marker without fetching or executing their contents;
- source and runtime sanitization agree on the supported contract;
- security limits are enforced at the app boundary;
- focused durable tests cover the new behavior;
- affected documentation describes the shipped behavior;
- no excluded capability was added implicitly as an unsafe side effect.
