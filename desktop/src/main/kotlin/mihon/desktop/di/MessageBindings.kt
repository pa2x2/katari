package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import mihon.core.platform.message.OutsideScreenMessages
import mihon.desktop.message.AppMessageOutsideScreenMessages

@BindingContainer
interface MessageBindings {
    @Binds
    val AppMessageOutsideScreenMessages.outsideScreenMessages: OutsideScreenMessages
}
