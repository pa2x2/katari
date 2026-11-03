package mihon.presentation.kit.content

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit

/*
 * Text styles for content renderers, such as the book reader, whose text looks the way a document's stylesheet and the
 * reader's text-size setting say: values that come from data, not design. App UI never builds a text style; it picks a
 * TextRole.
 */

/**
 * A paragraph of document text. Every value comes from the document or the reader's settings. [hyphenate] breaks words
 * at line ends in the paragraph's language, which justified text needs to avoid wide gaps between words.
 */
fun documentTextStyle(
    fontSize: TextUnit,
    lineHeight: TextUnit,
    firstLineIndent: TextUnit,
    bold: Boolean,
    fontFamily: FontFamily,
    textAlign: TextAlign,
    textDirection: TextDirection,
    localeList: LocaleList?,
    color: Color = Color.Unspecified,
    italic: Boolean = false,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    smallCaps: Boolean = false,
    shadow: Shadow? = null,
    hyphenate: Boolean = false,
): TextStyle = TextStyle(
    color = color,
    fontSize = fontSize,
    fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
    letterSpacing = letterSpacing,
    fontFeatureSettings = if (smallCaps) SMALL_CAPS else null,
    shadow = shadow,
    lineHeight = lineHeight,
    textIndent = TextIndent(firstLine = firstLineIndent),
    textDirection = textDirection,
    localeList = localeList,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    fontFamily = fontFamily,
    textAlign = textAlign,
    hyphens = if (hyphenate) Hyphens.Auto else Hyphens.None,
    lineBreak = if (hyphenate) LineBreak.Paragraph else LineBreak.Unspecified,
)

/**
 * A run inside document text that the document styles on its own, such as emphasis or a link. Values left at their
 * defaults keep the surrounding paragraph's style; [bold] and [italic] only ever add weight or slant.
 */
fun documentSpanStyle(
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    bold: Boolean = false,
    italic: Boolean = false,
    fontFamily: FontFamily? = null,
    textDecoration: TextDecoration? = null,
    baselineShift: BaselineShift? = null,
    localeList: LocaleList? = null,
    background: Color = Color.Unspecified,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    smallCaps: Boolean = false,
    shadow: Shadow? = null,
): SpanStyle = SpanStyle(
    color = color,
    background = background,
    letterSpacing = letterSpacing,
    fontFeatureSettings = if (smallCaps) SMALL_CAPS else null,
    shadow = shadow,
    fontSize = fontSize,
    fontWeight = if (bold) FontWeight.Bold else null,
    fontStyle = if (italic) FontStyle.Italic else null,
    fontFamily = fontFamily,
    textDecoration = textDecoration,
    baselineShift = baselineShift,
    localeList = localeList,
)

// OpenType small capitals; fonts without the feature draw ordinary lowercase.
private const val SMALL_CAPS = "smcp"
