## The name

“Katari” evokes the Japanese word _katari_ (語り), meaning storytelling or the telling of stories. The name reflects Katari's goal of bringing different kinds of stories into one library.

## Relationship with Mihon

Katari is an independent fork of [Mihon](https://mihon.app/). It retains much of Mihon's library, reader, download, tracking, source, and settings behavior while adding support for more kinds of entries and Katari-specific ways to organize and discover them.

Katari would not exist without Mihon and its contributors. Katari is not an official Mihon project, however, and the two projects have separate maintainers, releases, support channels, and priorities.

For features inherited unchanged, see [documentation for inherited features](./inherited-features.md). When Katari behaves differently, follow the Katari guide.

## Terminology

Mihon's documentation is written for manga and commonly uses terms such as **series** and **chapter**. Katari generalizes the underlying model:

| Mihon documentation | Katari meaning                                                                                                    |
| ------------------- | ----------------------------------------------------------------------------------------------------------------- |
| Series or manga     | Entry                                                                                                             |
| Chapter             | An openable unit within an entry                                                                                  |
| Read                | Consumed progress; the exact action depends on the entry type                                                     |
| Reader              | A type-specific interface for readable content; Mihon's reader documentation describes Katari's image reader only |

The interface may use a more specific term when the current entry type is known. Reader settings belong to their reader implementation, so instructions for the image reader do not automatically apply to a book reader or video player.

## Support and issue reporting

!!! warning

    Do not ask Mihon's maintainers or support community to diagnose Katari-specific behavior. Report problems through [github issues](https://github.com/katariapp/katari/issues), especially when they involve:

    - Profiles
    - Type-specific media and playback
    - Unified or merged entries
    - Katari feeds and discovery behavior
    - Katari backup data
    - Entry-native extensions
    - Katari builds, updates, or telemetry

When a problem appears in behavior inherited unchanged from Mihon, check the linked upstream documentation first. If it still occurs in Katari, report it to Katari with the app version, affected entry type, reproduction steps, and relevant logs. Katari's maintainers can determine whether the issue also exists upstream.

References to Mihon are attribution and guidance, not a claim of affiliation or a request for Mihon to support Katari.
