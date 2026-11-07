package app.katari.sync.server.web.page

import app.katari.sync.server.web.request.FormTokens
import kotlinx.html.ButtonType
import kotlinx.html.FORM
import kotlinx.html.FlowContent
import kotlinx.html.FormMethod
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.id
import kotlinx.html.input
import kotlinx.html.label
import kotlinx.html.p
import kotlinx.html.role

internal enum class ButtonStyle(val classes: String) {
    Filled("bt f"),
    Outlined("bt o"),
    Text("bt t"),
    Danger("bt o danger"),
    DangerFilled("bt ef"),
}

/** A form that changes something, carrying the visitor's [token] as every POST must ([FormTokens]). */
internal fun FlowContent.postForm(action: String, token: String, classes: String? = null, content: FORM.() -> Unit) {
    form(action = action, method = FormMethod.post, classes = classes) {
        input(InputType.hidden, name = FormTokens.FIELD) { value = token }
        content()
    }
}

internal fun FlowContent.field(
    label: String,
    name: String,
    autocomplete: String,
    type: InputType = InputType.text,
    value: String? = null,
    code: Boolean = false,
    hint: String? = null,
) {
    label("lbl") {
        htmlFor = name
        +label
    }
    input(type, name = name, classes = if (code) "in code" else "in") {
        id = name
        required = true
        attributes["autocomplete"] = autocomplete
        if (code || type == InputType.text) {
            attributes["autocapitalize"] = "none"
            attributes["spellcheck"] = "false"
        }
        value?.let { this.value = it }
    }
    hint?.let { p("hint") { +it } }
}

internal fun FlowContent.formError(message: String?) {
    message ?: return
    p("error") {
        role = "alert"
        +message
    }
}

internal fun FlowContent.submitButton(label: String, style: ButtonStyle, enabled: Boolean = true) {
    button(type = ButtonType.submit, classes = style.classes) {
        disabled = !enabled
        +label
    }
}

internal fun FlowContent.linkButton(label: String, href: String, style: ButtonStyle) {
    a(href = href, classes = style.classes) { +label }
}

/** A form's buttons, its main action last. */
internal fun FlowContent.actions(content: FlowContent.() -> Unit) {
    div("actions") { content() }
}

/** A one-time secret shown once, with a copy button the script reveals. */
internal fun FlowContent.secret(value: String) {
    p("secret") {
        id = "secret"
        +value
    }
    button(type = ButtonType.button, classes = "bt t copy") {
        attributes["data-copy"] = "secret"
        attributes["hidden"] = "hidden"
        +"Copy"
    }
}

/** The signed-in username, for password managers filling a password field of a form that doesn't ask for it. */
internal fun FlowContent.hiddenUsername(username: String) {
    input(InputType.text, name = "username", classes = "visually-hidden") {
        value = username
        readonly = true
        attributes["tabindex"] = "-1"
        attributes["autocomplete"] = "username"
        attributes["aria-hidden"] = "true"
    }
}
