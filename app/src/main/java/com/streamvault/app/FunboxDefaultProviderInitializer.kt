package com.streamvault.app

import com.streamvault.domain.model.M3uConfig
import com.streamvault.domain.repository.ProviderRepository
import com.streamvault.domain.repository.ProviderSetupRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/** Installs the built-in TV source once, while preserving a user's active provider. */
@Singleton
internal class FunboxDefaultProviderInitializer @Inject constructor(
    private val providerRepository: ProviderRepository
) {
    suspend fun ensureConfigured() {
        val existing = providerRepository.getProviders().first()
        if (existing.any { it.m3uUrl == DEFAULT_PLAYLIST_URL }) return

        val previousActiveProviderId = providerRepository.getActiveProvider().first()?.id
        try {
            providerRepository.setupProvider(
                ProviderSetupRequest.Configured(
                    name = DEFAULT_PROVIDER_NAME,
                    configuration = M3uConfig(playlistUrl = DEFAULT_PLAYLIST_URL)
                )
            )
        } finally {
            if (previousActiveProviderId != null) {
                providerRepository.setActiveProvider(previousActiveProviderId)
            }
        }
    }

    companion object {
        const val DEFAULT_PROVIDER_NAME = "FunBOX / עידן פלוס"
        const val DEFAULT_PLAYLIST_URL = "http://tiny.cc/FanTV"
    }
}
