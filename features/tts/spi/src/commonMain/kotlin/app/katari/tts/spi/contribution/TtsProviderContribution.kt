package app.katari.tts.spi.contribution

import app.katari.tts.api.engine.TtsProviderId

/**
 * A speech provider the build carries. A build without one can't read text aloud, so it offers no speech at all. A
 * provider whose engines are found on the device can have none at a time; speech stays offered then, so the user can
 * be told to install one.
 */
data class TtsProviderContribution(
    val id: TtsProviderId,
)
