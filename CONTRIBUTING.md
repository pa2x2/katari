# Contributing to Katari

The public [developer documentation](https://katariapp.github.io/katari/developers/) explains how extension authors use the Entry SDK. This guide covers repository-specific work on the SDK and its documentation.

## Preview the documentation

From the repository root, run:

```bash
./scripts/serve-docs.sh
```

The script installs the pinned VitePress dependencies, generates the Entry SDK API reference with Dokka, stages it with the Markdown documentation, and starts the local VitePress server. It uses an installed pnpm or Corepack when available and can bootstrap the pinned pnpm version with Bun. Open <http://localhost:8000/katari/> to review the complete site, including the API reference.

Normal `vitepress dev` arguments can be passed to the script:

```bash
./scripts/serve-docs.sh --host 127.0.0.1 --port 9000
```

The generated API reference is labeled `development` by default. Set `SDK_DOC_VERSION` when an exact label is useful:

```bash
SDK_DOC_VERSION=sdk-2.0.0 ./scripts/serve-docs.sh
```

## Change the public Entry SDK

For a public API change:

1. Classify it as patch, minor, or major according to the [SDK versioning policy](https://katariapp.github.io/katari/developers/sdk/versioning).
2. Add KDoc for every new or changed public declaration and member.
3. Update the relevant concept, content-type, or capability guide.
4. Add focused SDK tests and app-side tests for host recognition and fallback behavior.
5. Publish `local-SNAPSHOT` and compile a representative extension against it.
6. Test the extension with the matching Katari runtime rather than only compiling it.
7. Record the change and the first supporting Katari version in the SDK changelog.
8. Verify the generated API reference and production documentation build.

When adding a content type, define its user-facing meaning, child-item semantics, media contract, applicable capabilities, runtime behavior, compatibility level, and minimum Katari version.

An SDK artifact and its runtime support must be released coherently. Do not publish a stable SDK tag for a symbol absent from the corresponding Katari runtime, and do not present an unreleased symbol as available in an older stable artifact.
