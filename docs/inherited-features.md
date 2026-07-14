# Documentation for inherited features

Katari retains many of Mihon's established workflows. Use the upstream guides below when Katari does not document a difference.

::: warning Katari is an independent project

These links lead to Mihon's website. Mihon's documentation is useful for inherited behavior, but Mihon's maintainers and support channels are not responsible for Katari. Report Katari problems to Katari.

:::

Read [About project](./about-project.md) for terminology and support information.

## Shared guides

These areas are substantially inherited where the corresponding Katari feature uses the same workflow:

| Topic                | Mihon documentation                                              | Katari scope note                                                                                                      |
| -------------------- | ---------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| Reader configuration | [Reader settings](https://mihon.app/docs/guides/reader-settings) | Applies to Katari's image reader, not its video player.                                                                |
| Categories           | [Categories](https://mihon.app/docs/guides/categories)           | Basic category management is inherited; see [Profiles](./features/profiles.md) for profile-specific behavior.          |
| Tracking             | [Tracking](https://mihon.app/docs/guides/tracking)               | Shared tracker workflows apply unless Katari documents entry-type behavior separately.                                 |
| Library questions    | [Library FAQ](https://mihon.app/docs/faq/library)                | Some wording is manga-specific; apply it to compatible Katari entries only.                                            |
| Downloads            | [Downloads FAQ](https://mihon.app/docs/faq/downloads)            | Shared download behavior primarily covers image chapters.                                                              |
| Storage              | [Storage FAQ](https://mihon.app/docs/faq/storage)                | Android storage guidance is shared; Katari uses its own application identity and folder naming where shown by the app. |

For additional help, see Mihon's [troubleshooting guide](https://mihon.app/docs/guides/troubleshooting/) and [settings FAQ](https://mihon.app/docs/faq/settings). A Mihon statement about features the project does not support is not necessarily a statement about Katari.

## Shared foundation with Katari differences

Some workflows begin with inherited Mihon behavior but involve additional Katari data or semantics. Consult the upstream page for the common operation, then check Katari documentation before relying on its compatibility details.

| Topic              | Shared foundation                                                        | Why Katari needs its own guidance                                                                                                       |
| ------------------ | ------------------------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------- |
| Backup and restore | [Mihon backups](https://mihon.app/docs/guides/backups)                   | Katari can include profiles, additional entry types, merged-entry state, and other fork-specific data.                                  |
| Source migration   | [Mihon source migration](https://mihon.app/docs/guides/source-migration) | Katari migration can be entry-type-aware and interact with unified or merged entries.                                                   |
| Extensions         | [Mihon extension FAQ](https://mihon.app/docs/faq/browse/extensions)      | Katari supports selected legacy extensions but uses its Entry API for new extension development.                                        |
| Getting started    | [Mihon getting started](https://mihon.app/docs/guides/getting-started)   | Basic source and library concepts are familiar, but installation, supported media, navigation, and Katari-specific features can differ. |

The linked Mihon guides describe the shared foundation. Katari-specific behavior is described in the corresponding pages on this site.
