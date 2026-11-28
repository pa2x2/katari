package eu.kanade.tachiyomi.data.backup.conversion.lnreader.settings

import app.katari.entry.interactions.reader.settings.BookTextRule
import app.katari.entry.interactions.reader.settings.BookTextRuleScope
import eu.kanade.tachiyomi.data.backup.conversion.reportName
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import tachiyomi.i18n.*
import java.util.UUID

/**
 * LNReader's Custom Code: its text rules apply to every novel, and Katari's to one source or series, so each becomes a
 * rule for every source of the converted backup (D084). Its CSS and JavaScript have nothing to run in Katari and are
 * listed.
 */
internal fun LnReaderSettings.convertCustomCode(settings: LnReaderChapterReaderSettings, sources: List<BackupSource>) {
    convertTextRules(settings, sources)
    if (!settings.customCSS.isNullOrBlank() || !settings.codeSnippetsCSS.isNullOrEmpty()) {
        leaveBehind(LnReaderSettingLabels.CSS_SNIPPETS, MR.strings.backup_conversion_lnreader_custom_code)
    }
    if (!settings.customJS.isNullOrBlank() || !settings.codeSnippetsJS.isNullOrEmpty()) {
        leaveBehind(LnReaderSettingLabels.JS_SNIPPETS, MR.strings.backup_conversion_lnreader_custom_code)
    }
}

private fun LnReaderSettings.convertTextRules(settings: LnReaderChapterReaderSettings, sources: List<BackupSource>) {
    val finds = settings.removeText.orEmpty().map { it to null } + settings.replaceText.orEmpty().toList()
    val rules = finds.filter { (find, _) ->
        find.isNotBlank()
    }.map { (find, replaceWith) -> TextRule(find, replaceWith) }
    if (rules.isEmpty()) return
    if (sources.isEmpty()) {
        leaveBehind(LnReaderSettingLabels.TEXT_RULES, MR.strings.backup_conversion_lnreader_text_rules_no_source)
        return
    }
    val katariRules = sources.flatMap { source ->
        val scope = BookTextRuleScope.Source(source.sourceId, source.reportName)
        rules.map { it.forSource(scope) }
    }
    write(
        TEXT_RULES_KEY,
        StringPreferenceValue(BookTextRule.encodeList(katariRules)),
        if (rules.any { it.dropsFlags }) {
            MR.strings.backup_conversion_lnreader_text_rules_flags
        } else {
            MR.strings.backup_conversion_lnreader_text_rules_per_source
        },
    )
}

/** One of LNReader's rules: [text] is plain, or `/pattern/flags` for a regular expression. */
private class TextRule(private val text: String, private val replaceWith: String?) {
    private val regex = REGEX.matchEntire(text)

    val dropsFlags: Boolean = !regex?.groupValues?.get(2).isNullOrEmpty()

    fun forSource(scope: BookTextRuleScope.Source) = BookTextRule(
        id = UUID.randomUUID().toString(),
        find = regex?.groupValues?.get(1) ?: text,
        scope = scope,
        regex = regex != null,
        replaceWith = replaceWith,
    )

    private companion object {
        // isRegexString of src/utils/customCode.ts
        val REGEX = Regex("""^/(.*)/([gmiyuvsd]*)$""")
    }
}

// BookDocumentReaderPreferences' key
private const val TEXT_RULES_KEY = "book_document_reader_text_rules"
