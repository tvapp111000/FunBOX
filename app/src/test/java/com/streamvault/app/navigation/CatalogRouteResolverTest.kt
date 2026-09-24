package com.streamvault.app.navigation

import com.google.common.truth.Truth.assertThat
import com.streamvault.core.navigation.AppDestination
import com.streamvault.domain.model.CatalogLayout
import com.streamvault.domain.model.ContentType
import org.junit.Test

class CatalogRouteResolverTest {
    @Test
    fun unifiedAndUnknownLayoutsPreserveExplicitCatalogRoute() {
        assertThat(resolveCatalogDestination(CatalogLayout.UNIFIED_VOD, AppDestination.Movies, ContentType.MOVIE, true))
            .isEqualTo(AppDestination.Movies)
        assertThat(resolveCatalogDestination(CatalogLayout.UNKNOWN, AppDestination.Series, ContentType.SERIES, true))
            .isEqualTo(AppDestination.Series)
    }

    @Test
    fun splitLayoutWaitsForDestinationPreferenceBeforeRedirecting() {
        assertThat(resolveCatalogDestination(CatalogLayout.SPLIT, AppDestination.Vod, ContentType.SERIES, false))
            .isEqualTo(AppDestination.Vod)
        assertThat(resolveCatalogDestination(CatalogLayout.SPLIT, AppDestination.Vod, ContentType.SERIES, true))
            .isEqualTo(AppDestination.Series)
    }
}
