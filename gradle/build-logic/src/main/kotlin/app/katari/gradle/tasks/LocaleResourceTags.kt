package app.katari.gradle.tasks

import java.io.File

/**
 * The language tags of the languages i18n has strings in, sorted, from its string files: one per resource folder that
 * isn't empty, `base` being English and Android's `-r` and `b+` qualifiers written as tags. Android's
 * `locales_config.xml` ([GenerateLocalesConfigTask]) and the app's language list ([GenerateAppLocaleTagsTask]) both
 * list these, so the app offers the languages Android knows it has.
 */
internal fun localeResourceTags(localeFiles: Iterable<File>): List<String> = localeFiles.asSequence()
    .filterNot { it.readText().contains(emptyResourcesElement) }
    .map {
        it.parentFile.name
            .replace("base", "en")
            .replace("-r", "-")
            .replace("+", "-")
    }
    .distinct()
    .sorted()
    .toList()

private val emptyResourcesElement = "<resources>\\s*</resources>|<resources\\s*/>".toRegex()
