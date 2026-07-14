package mihon.entry.interactions.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mihon.entry.viewer.settings.ViewerSettingsInteraction
import mihon.entry.viewer.settings.ViewerSettingsProvider

class DefaultViewerSettingsInteraction(
    providers: Collection<ViewerSettingsProvider>,
) : ViewerSettingsInteraction {
    private val providersById = providers.associateBy(ViewerSettingsProvider::id).toMutableMap()

    init {
        providers.forEach(::validateProvider)
        require(providersById.size == providers.size) {
            "Duplicate viewer settings provider IDs: " +
                providers.groupingBy(ViewerSettingsProvider::id).eachCount().filterValues { it > 1 }.keys
        }
    }

    private val mutableProviders = MutableStateFlow(providersById.values.sortedBy { it.displayName })
    override val providers: StateFlow<List<ViewerSettingsProvider>> = mutableProviders.asStateFlow()

    @Synchronized
    override fun provider(id: String): ViewerSettingsProvider? = providersById[id]

    @Synchronized
    override fun register(provider: ViewerSettingsProvider) {
        validateProvider(provider)
        check(provider.id !in providersById) { "Duplicate viewer settings provider ID: ${provider.id}" }
        providersById[provider.id] = provider
        publishProviders()
    }

    @Synchronized
    override fun unregister(providerId: String) {
        providersById.remove(providerId)
        publishProviders()
    }

    private fun publishProviders() {
        mutableProviders.value = providersById.values.sortedBy { it.displayName }
    }

    private fun validateProvider(provider: ViewerSettingsProvider) {
        require(provider.id.isNotBlank()) { "Viewer settings provider ID must not be blank" }
        require(provider.id.length <= 200) { "Viewer settings provider ID is too long" }
        require(provider.displayName.isNotBlank()) { "Viewer settings provider name must not be blank" }
        require(provider.isAvailable || !provider.unavailableReason.isNullOrBlank()) {
            "Unavailable viewer settings provider ${provider.id} must explain why it is unavailable"
        }
        require(provider.settings.map { it.id }.distinct().size == provider.settings.size) {
            "Duplicate settings registered by ${provider.id}"
        }
        require(provider.settings.all { it.id.providerId == provider.id }) {
            "Viewer setting IDs must match their provider ID"
        }
    }
}
