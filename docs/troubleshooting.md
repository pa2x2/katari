# Troubleshooting

## Is the problem specific to Katari?

A problem is likely Katari-specific when it involves:

- [Profiles](./features/profiles.md), including profile-specific settings or data
- An entry type, book reader, or video player that Mihon does not provide
- [Merged entries](./features/merged-entries.md)
- [Feeds and discovery](./features/feeds-and-discovery.md)
- Katari's Entry extensions or compatibility with a Mihon extension
- Katari-specific [backup and restore data](./differences/backup-and-restore.md)
- Katari builds, updates, or telemetry

For these problems:

1. Update to the [latest Katari release](https://github.com/katariapp/katari/releases/latest) and try again.
2. Note the active profile, affected entry type, source, and extension version where applicable.
3. Check whether the problem affects one entry or source, or every entry of that type.
4. For a crash, open **More → Settings → Advanced**, select **Dump crash logs**, and review the file for private information before sharing it.
5. Search [existing Katari issues](https://github.com/katariapp/katari/issues) before creating an [issue report](https://github.com/katariapp/katari/issues/new?template=2_report_issue.yml).

Source-site or extension-content failures may need to be reported to the source or extension maintainer. Do not include credentials, tokens, cookies, or signed media URLs in an issue.

## Troubleshooting inherited behavior

Katari inherits many Mihon workflows, including common source, WebView, Cloudflare, download, storage, tracking, and image-reader behavior. For a problem in one of those areas, continue with:

- [Mihon's troubleshooting guide](https://mihon.app/docs/guides/troubleshooting/) for WebView, Cloudflare, logs, and installation problems
- [Mihon's common issues guide](https://mihon.app/docs/guides/troubleshooting/common-issues) for common source, update, download, and reader problems
- [Mihon's FAQ](https://mihon.app/docs/faq/general) for feature-specific questions

Mihon's wording is manga-oriented. Apply **series** to a Katari entry and **chapter** to its openable child item where the inherited workflow is the same.

::: warning Katari is an independent project

Use Mihon's documentation for inherited behavior, but do not ask Mihon's maintainers or support community to diagnose Katari. If the problem still occurs in Katari, report it to Katari with the details listed above.

:::
