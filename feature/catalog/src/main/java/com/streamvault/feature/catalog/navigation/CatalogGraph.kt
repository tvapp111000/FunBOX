package com.streamvault.feature.catalog.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.streamvault.core.navigation.AppDestination
import com.streamvault.core.navigation.NavigationActions
import com.streamvault.domain.model.Channel
import com.streamvault.domain.model.ContentType
import com.streamvault.data.remote.clipbox.ClipboxMediaType
import com.streamvault.data.remote.clipbox.ClipboxTitle
import com.streamvault.domain.model.Episode
import com.streamvault.domain.model.Movie
import com.streamvault.domain.model.PlaybackHistory
import com.streamvault.domain.model.Series
import com.streamvault.feature.catalog.api.CatalogChannelPlaybackContext
import com.streamvault.feature.catalog.api.CatalogDashboardShelfCustomizationContent
import com.streamvault.feature.catalog.api.CatalogPlatformHost
import com.streamvault.feature.catalog.api.CatalogScaffoldContent
import com.streamvault.feature.catalog.presentation.clipbox.ClipboxBrowseKind
import com.streamvault.feature.catalog.presentation.clipbox.ClipboxBrowseScreen
import com.streamvault.feature.catalog.presentation.clipbox.ClipboxDetailScreen
import com.streamvault.feature.catalog.presentation.clipbox.ClipboxAccountScreen
import com.streamvault.feature.catalog.presentation.favorites.FavoritesScreen
import com.streamvault.feature.catalog.presentation.movies.MovieDetailScreen
import com.streamvault.feature.catalog.presentation.search.SearchScreen
import com.streamvault.feature.catalog.presentation.series.SeriesDetailScreen
import com.streamvault.feature.catalog.presentation.vod.VodScreen

/**
 * Registers every Catalog-owned destination without depending on the app navigation
 * implementation. App composition supplies platform, payload, player, and shell seams.
 */
fun NavGraphBuilder.registerCatalogGraph(
    actions: NavigationActions,
    platformHost: CatalogPlatformHost?,
    scaffold: CatalogScaffoldContent,
    dashboardShelfCustomizationContent: CatalogDashboardShelfCustomizationContent,
    onTopLevelDestinationRequested: (AppDestination) -> Unit,
    onOpenMovieDetail: (Movie, AppDestination) -> Unit,
    onOpenSeriesDetail: (Series, AppDestination) -> Unit,
    onPlayChannel: (Channel, CatalogChannelPlaybackContext) -> Unit,
    onPlayMovie: (Movie, AppDestination) -> Unit,
    onPlayEpisode: (Episode, AppDestination) -> Unit,
    onPlayHistory: (PlaybackHistory, AppDestination) -> Unit,
    consumeMoviePresentationHint: (NavBackStackEntry) -> Unit,
    consumeSeriesPresentationHint: (NavBackStackEntry) -> Unit,
    decodeDestination: (String) -> AppDestination? = ::decodeCatalogReturnDestination,
) {
    fun openClipboxTitle(title: ClipboxTitle, returnDestination: AppDestination) {
        val destination = if (title.type == ClipboxMediaType.MOVIE) {
            AppDestination.ClipboxMovieDetail(title.id, returnDestination)
        } else {
            AppDestination.ClipboxSeriesDetail(title.id, returnDestination)
        }
        actions.navigate(destination)
    }

    composable(CatalogRoutePatterns.HOME) {
        ClipboxBrowseScreen(
            kind = ClipboxBrowseKind.HOME,
            onTitleClick = { title -> openClipboxTitle(title, AppDestination.Home) },
            onAccountClick = { actions.navigate(AppDestination.ClipboxAccount) },
            scaffold = scaffold,
        )
    }

    composable(CatalogRoutePatterns.CLIPBOX_ACCOUNT) {
        ClipboxAccountScreen(
            onBack = { actions.returnTo(AppDestination.Home) },
            scaffold = scaffold,
        )
    }

    composable(CatalogRoutePatterns.FAVORITES) {
        FavoritesScreen(
            onClipboxTitleClick = { title -> openClipboxTitle(title, AppDestination.Favorites) },
            onItemClick = { item ->
                when (item.favorite.contentType) {
                    ContentType.LIVE -> onPlayChannel(
                        Channel(
                            id = item.favorite.contentId,
                            name = item.title,
                            providerId = item.providerId,
                            streamUrl = item.streamUrl,
                            categoryId = item.categoryId,
                            epgChannelId = item.epgChannelId,
                        ),
                        CatalogChannelPlaybackContext(
                            categoryId = item.launchCategoryId ?: item.categoryId,
                            providerId = item.providerId,
                            isVirtual = item.launchIsVirtual,
                            combinedProfileId = null,
                            returnDestination = AppDestination.Favorites,
                        )
                    )
                    ContentType.MOVIE -> actions.navigate(
                        AppDestination.MovieDetail(item.favorite.contentId, AppDestination.Favorites)
                    )
                    ContentType.SERIES -> actions.navigate(
                        AppDestination.SeriesDetail(item.favorite.contentId, AppDestination.Favorites)
                    )
                    else -> Unit
                }
            },
            onHistoryClick = { item -> onPlayHistory(item.history, AppDestination.Favorites) },
            currentDestination = AppDestination.Favorites,
            onDestinationRequested = onTopLevelDestinationRequested,
            scaffold = scaffold,
        )
    }

    composable(CatalogRoutePatterns.MOVIES) {
        ClipboxBrowseScreen(
            kind = ClipboxBrowseKind.MOVIES,
            onTitleClick = { title -> openClipboxTitle(title, AppDestination.Movies) },
            scaffold = scaffold,
        )
    }

    composable(CatalogRoutePatterns.SERIES) {
        ClipboxBrowseScreen(
            kind = ClipboxBrowseKind.SERIES,
            onTitleClick = { title -> openClipboxTitle(title, AppDestination.Series) },
            scaffold = scaffold,
        )
    }

    composable(CatalogRoutePatterns.VOD) {
        VodScreen(
            onMovieClick = { movie -> onOpenMovieDetail(movie, AppDestination.Vod) },
            onSeriesClick = { series -> onOpenSeriesDetail(series, AppDestination.Vod) },
            scaffold = scaffold,
        )
    }

    composable(
        route = CatalogRoutePatterns.SEARCH_DESTINATION,
        arguments = listOf(
            navArgument("query") { type = NavType.StringType; defaultValue = "" }
        )
    ) { backStackEntry ->
        val query = backStackEntry.arguments?.getString("query").orEmpty()
        SearchScreen(
            initialQuery = query,
            onChannelClick = { channel ->
                onPlayChannel(
                    channel,
                    CatalogChannelPlaybackContext(
                        categoryId = channel.categoryId,
                        providerId = channel.providerId,
                        isVirtual = false,
                        combinedProfileId = null,
                        returnDestination = AppDestination.Search(query),
                    )
                )
            },
            onMovieClick = { movie -> onOpenMovieDetail(movie, AppDestination.Search(query)) },
            onSeriesClick = { series -> onOpenSeriesDetail(series, AppDestination.Search(query)) },
            onClipboxTitleClick = { title -> openClipboxTitle(title, AppDestination.Search(query)) },
            scaffold = scaffold,
        )
    }

    composable(
        route = CatalogRoutePatterns.MOVIE_DETAIL,
        arguments = listOf(
            navArgument("movieId") { type = NavType.LongType },
            navArgument("returnRoute") { type = NavType.StringType; defaultValue = "" },
        )
    ) { backStackEntry ->
        consumeMoviePresentationHint(backStackEntry)
        val returnDestination = backStackEntry.arguments?.getString("returnRoute")
            .orEmpty()
            .takeIf(String::isNotBlank)
            ?.let(decodeDestination)
        val movieId = backStackEntry.arguments?.getLong("movieId") ?: -1L
        MovieDetailScreen(
            onPlay = { movie ->
                onPlayMovie(
                    movie,
                    AppDestination.MovieDetail(
                        movieId = movie.id.takeIf { it > 0L } ?: movieId,
                        returnDestination = returnDestination,
                    )
                )
            },
            onOpenRelatedMovie = { relatedMovie ->
                openRelatedMovieDetail(
                    relatedMovie = relatedMovie,
                    currentMovieId = movieId,
                    currentReturnDestination = returnDestination,
                    onOpenMovieDetail = onOpenMovieDetail,
                )
            },
            onBack = { actions.returnTo(returnDestination) },
            platformHost = platformHost,
        )
    }

    composable(
        route = CatalogRoutePatterns.SERIES_DETAIL,
        arguments = listOf(
            navArgument("seriesId") { type = NavType.LongType },
            navArgument("returnRoute") { type = NavType.StringType; defaultValue = "" },
        )
    ) { backStackEntry ->
        consumeSeriesPresentationHint(backStackEntry)
        val returnDestination = backStackEntry.arguments?.getString("returnRoute")
            .orEmpty()
            .takeIf(String::isNotBlank)
            ?.let(decodeDestination)
        val seriesId = backStackEntry.arguments?.getLong("seriesId") ?: -1L
        SeriesDetailScreen(
            onEpisodeClick = { episode ->
                onPlayEpisode(
                    episode,
                    AppDestination.SeriesDetail(
                        seriesId = episode.seriesId.takeIf { it > 0L } ?: seriesId,
                        returnDestination = returnDestination,
                    )
                )
            },
            onBack = { actions.returnTo(returnDestination) },
            platformHost = platformHost,
        )
    }

    composable(
        route = CatalogRoutePatterns.CLIPBOX_MOVIE_DETAIL,
        arguments = listOf(
            navArgument("movieId") { type = NavType.LongType },
            navArgument("returnRoute") { type = NavType.StringType; defaultValue = "" },
        ),
    ) { entry ->
        val id = entry.arguments?.getLong("movieId") ?: -1L
        val returnDestination = entry.arguments?.getString("returnRoute")
            ?.takeIf(String::isNotBlank)?.let(decodeDestination)
        ClipboxDetailScreen(
            id = id,
            type = ClipboxMediaType.MOVIE,
            onBack = { actions.returnTo(returnDestination) },
            scaffold = scaffold,
        )
    }

    composable(
        route = CatalogRoutePatterns.CLIPBOX_SERIES_DETAIL,
        arguments = listOf(
            navArgument("seriesId") { type = NavType.LongType },
            navArgument("returnRoute") { type = NavType.StringType; defaultValue = "" },
        ),
    ) { entry ->
        val id = entry.arguments?.getLong("seriesId") ?: -1L
        val returnDestination = entry.arguments?.getString("returnRoute")
            ?.takeIf(String::isNotBlank)?.let(decodeDestination)
        ClipboxDetailScreen(
            id = id,
            type = ClipboxMediaType.SERIES,
            onBack = { actions.returnTo(returnDestination) },
            scaffold = scaffold,
        )
    }
}

internal fun openRelatedMovieDetail(
    relatedMovie: Movie,
    currentMovieId: Long,
    currentReturnDestination: AppDestination?,
    onOpenMovieDetail: (Movie, AppDestination) -> Unit,
) {
    onOpenMovieDetail(
        relatedMovie,
        AppDestination.MovieDetail(
            movieId = currentMovieId,
            returnDestination = currentReturnDestination,
        )
    )
}

private fun decodeCatalogReturnDestination(route: String): AppDestination? {
    val path = route.substringBefore('?')
    return when (path) {
        CatalogRoutePatterns.HOME -> AppDestination.Home
        CatalogRoutePatterns.MOVIES -> AppDestination.Movies
        CatalogRoutePatterns.SERIES -> AppDestination.Series
        CatalogRoutePatterns.FAVORITES -> AppDestination.Favorites
        CatalogRoutePatterns.VOD -> AppDestination.Vod
        CatalogRoutePatterns.SEARCH -> AppDestination.Search(
            route.substringAfter("query=", "").takeIf(String::isNotBlank)
        )
        else -> null
    }
}
