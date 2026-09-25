package com.streamvault.app.di

import com.streamvault.app.BuildConfig
import com.streamvault.data.remote.clipbox.ClipboxApi
import com.streamvault.data.remote.clipbox.ClipboxCredentials
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ClipboxModule {
    @Provides
    @Singleton
    fun provideClipboxCredentials(): ClipboxCredentials = ClipboxCredentials(
        appKey = BuildConfig.CLIPBOX_API_KEY,
        signingDigest = BuildConfig.CLIPBOX_SIGNING_DIGEST,
    )

    @Provides
    @Singleton
    fun provideClipboxApi(credentials: ClipboxCredentials): ClipboxApi = ClipboxApi(credentials)
}
