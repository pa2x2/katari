package mihon.desktop.message

import dev.zacsweers.metro.Inject
import mihon.core.platform.message.OutsideScreenMessages
import mihon.presentation.kit.feedback.AppMessages

@Inject
class AppMessageOutsideScreenMessages(private val messages: AppMessages) : OutsideScreenMessages {
    override suspend fun show(text: String, long: Boolean) {
        messages.show(text, long)
    }
}
