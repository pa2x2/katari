package app.katari.webpages.page

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

/**
 * How a script's promise reaches the app. A web view answers a script with its value at once, and a promise's value
 * comes later, so the page has to send it: [script] runs a script as one expression, answers at once with what it
 * gave ([started]) and, where that was a promise, posts what the promise settled as to the page's [channel] ([read]).
 *
 * Only a script that is one expression can be run so: nothing in a page takes hold of the value of a script of
 * several statements, short of compiling it from text, which a site's content security policy may forbid.
 */
internal class ExpressionValues(private val channel: String) {
    private val mark = randomLetters(MARK_LETTERS)

    /** [expression] wrapped to answer as [started] reads; it fails to parse where [expression] isn't an expression. */
    fun script(expression: String, id: Int): String {
        // The expression stands outside the function that handles its value, where none of that function's names
        // hide the page's own from it. What ends a statement would end the expression too early.
        val bare = expression.trimEnd { it.isWhitespace() || it == ';' }
        return """
            ((expression) => {
                const text = (value) => {
                    try {
                        const text = JSON.stringify(value);
                        return text === undefined ? null : text;
                    } catch (unheld) {
                        return null;
                    }
                };
                let value;
                try {
                    value = expression();
                } catch (thrown) {
                    return [$THREW, String(thrown)];
                }
                if (!value || typeof value.then !== 'function') return [$VALUE, text(value)];
                const settled = (kind, what) => {
                    window[${JsonPrimitive(
            channel,
        )}].postMessage(JSON.stringify([${JsonPrimitive(mark)}, $id, kind, what]));
                };
                Promise.resolve(value).then(
                    (value) => settled($VALUE, text(value)),
                    (thrown) => settled($THREW, String(thrown)),
                );
                return [$PENDING];
            })(() => (
            $bare
            ))
        """.trimIndent()
    }

    /** What the script answered at once; null where it didn't run, as it wasn't an expression. */
    fun started(answer: String): Outcome? {
        val parts = runCatching { Json.parseToJsonElement(answer) }.getOrNull() as? JsonArray ?: return null
        return outcome(parts.getOrNull(0), parts.getOrNull(1), parts.size)
    }

    /** The promise [message] tells of, with the number its script was given; null for any other post. */
    fun read(message: String): Settled? {
        val post = runCatching { Json.parseToJsonElement(message) }.getOrNull() as? JsonArray ?: return null
        if ((post.getOrNull(0) as? JsonPrimitive)?.contentOrNull != mark) return null
        val id = (post.getOrNull(1) as? JsonPrimitive)?.intOrNull ?: return null
        val outcome = outcome(post.getOrNull(2), post.getOrNull(3), post.size - 2) as? Outcome.Final ?: return null
        return Settled(id, outcome)
    }

    private fun outcome(kind: Any?, what: Any?, parts: Int): Outcome? {
        val text = (what as? JsonPrimitive)?.contentOrNull
        return when ((kind as? JsonPrimitive)?.intOrNull) {
            VALUE -> Outcome.Value(text ?: NO_VALUE).takeIf { parts == 2 }
            THREW -> Outcome.Threw(text.orEmpty()).takeIf { parts == 2 }
            PENDING -> Outcome.Pending.takeIf { parts == 1 }
            else -> null
        }
    }

    sealed interface Outcome {
        sealed interface Final : Outcome

        /** @property json the value as JSON, `null` for one JSON can't hold. */
        class Value(val json: String) : Final

        class Threw(val text: String) : Final

        /** The value is a promise, whose own outcome the page posts. */
        data object Pending : Outcome
    }

    class Settled(val id: Int, val outcome: Outcome.Final)

    private companion object {
        const val MARK_LETTERS = 24
        const val VALUE = 0
        const val THREW = 1
        const val PENDING = 2
        const val NO_VALUE = "null"
    }
}
