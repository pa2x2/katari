# Extensions and compatibility

Katari can load selected Mihon and Keiyoushi extension API families for compatibility, but new Katari extensions use the Entry API.

## Legacy Mihon and Keiyoushi extensions

Installed extensions declaring supported Mihon or Keiyoushi library families can continue to provide manga sources. Katari verifies the original Mihon 1.4 and 1.6 surfaces plus the supported Keiyoushi 1.6 compatibility additions. An APK using other fork-specific APIs may install but fail to load.

Legacy sources are adapted into Katari's unified source model and produce the entry type represented by their legacy contract. They do not gain additional media behavior automatically.

Katari treats extension signatures as trust identities. A first installation may require trust confirmation. Updates must retain the package name and signing certificate.

## Public extension repository

Katari's public extensions and repository setup instructions are available in [`katari-extensions`](https://github.com/katariapp/katari-extensions). Extensions remain separate from the app and do not bundle or host content.

## Entry-native extensions

New extensions should use `entry-source-api`. Entry-native sources can:

- Return entries of different supported types from the same catalogue
- Resolve image pages or playback streams
- Provide subtitles, static title preview images, filters, preferences, URL resolution, and orientation capabilities. Preview-image consumption is currently limited to anime entries.
- Participate in immersive catalogue and feed browsing when supported

Start with the [extension development overview](../extensions/).

## Compatibility families

An extension's structured `versionName` declares its API family and extension revision. Katari validates the derived family before loading classes. See [SDK compatibility and versioning](../developers/sdk/versioning.md).

## Troubleshooting

If an extension does not appear or load, check that it:

- Declares the `tachiyomi.extension` feature and the correct source or factory class
- Uses a supported compatibility family
- Is signed and trusted
- Is allowed by the adult-content preference when applicable
- Does not package its own conflicting copy of the SDK

Source-site failures, extension-loader failures, and Katari app failures have different causes. Include the extension package and version, Katari version, affected source, and loader error when reporting a compatibility problem.
