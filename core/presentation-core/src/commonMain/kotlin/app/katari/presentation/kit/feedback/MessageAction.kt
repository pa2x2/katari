package app.katari.presentation.kit.feedback

/** What a message offers to do about what it says, such as opening the page that explains it: [label] names it. */
class MessageAction(val label: String, val onClick: () -> Unit)
