package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import mihon.app.message.ToastOutsideScreenMessages
import mihon.core.platform.message.OutsideScreenMessages

@BindingContainer
interface MessageBindings {
    @Binds
    val ToastOutsideScreenMessages.outsideScreenMessages: OutsideScreenMessages
}
