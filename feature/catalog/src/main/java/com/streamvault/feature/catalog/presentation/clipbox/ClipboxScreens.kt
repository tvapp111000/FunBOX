package com.streamvault.feature.catalog.presentation.clipbox

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.streamvault.core.navigation.AppDestination
import com.streamvault.core.ui.interaction.TvButton
import com.streamvault.core.ui.interaction.TvClickableSurface
import com.streamvault.data.remote.clipbox.ClipboxMediaType
import com.streamvault.data.remote.clipbox.ClipboxTitle
import com.streamvault.feature.catalog.api.CatalogNavigationChrome
import com.streamvault.feature.catalog.api.CatalogScaffoldContent

@Composable
fun ClipboxBrowseScreen(
    kind: ClipboxBrowseKind,
    onTitleClick: (ClipboxTitle) -> Unit,
    scaffold: CatalogScaffoldContent,
    viewModel: ClipboxBrowseViewModel = hiltViewModel(),
) {
    LaunchedEffect(kind) { viewModel.open(kind) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val destination = when (kind) {
        ClipboxBrowseKind.HOME -> AppDestination.Home
        ClipboxBrowseKind.MOVIES -> AppDestination.Movies
        ClipboxBrowseKind.SERIES -> AppDestination.Series
    }
    val heading = when (kind) {
        ClipboxBrowseKind.HOME -> "בית"
        ClipboxBrowseKind.MOVIES -> "סרטים"
        ClipboxBrowseKind.SERIES -> "סדרות"
    }
    scaffold(destination, heading, null, CatalogNavigationChrome.TopBar, true, true, false) {
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            if (kind == ClipboxBrowseKind.HOME) {
                val featured = state.shelves.firstOrNull()?.items?.firstOrNull()
                if (featured != null) item(key = "hero") { ClipboxHero(featured, onTitleClick) }
                state.shelves.forEachIndexed { index, shelf ->
                    item(key = "shelf:$index") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(shelf.title, style = MaterialTheme.typography.headlineSmall)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(shelf.items, key = { "${it.type}:${it.id}" }) { title ->
                                    ClipboxPoster(title, onTitleClick)
                                }
                            }
                        }
                    }
                }
            } else {
                state.titles.chunked(6).forEachIndexed { index, row ->
                    item(key = "row:$index") {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            row.forEach { ClipboxPoster(it, onTitleClick) }
                        }
                    }
                }
            }
            if (state.loading) item(key = "loading") { Text("טוען תוכן…") }
            state.error?.let { message ->
                item(key = "error") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(message)
                        TvButton(onClick = viewModel::loadMore) { Text("נסה שוב") }
                    }
                }
            }
            if (kind != ClipboxBrowseKind.HOME && state.titles.isNotEmpty() && !state.loading) {
                item(key = "more") { TvButton(onClick = viewModel::loadMore) { Text("טען עוד") } }
            }
        }
    }
}

@Composable
private fun ClipboxHero(title: ClipboxTitle, onTitleClick: (ClipboxTitle) -> Unit) {
    TvClickableSurface(
        onClick = { onTitleClick(title) },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(18.dp)),
        modifier = Modifier.fillMaxWidth().height(280.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(
                model = title.backdropUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Column(
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.65f))
                    .padding(18.dp),
            ) {
                Text(title.title, style = MaterialTheme.typography.headlineMedium)
                Text(title.overview, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ClipboxPoster(title: ClipboxTitle, onTitleClick: (ClipboxTitle) -> Unit) {
    Column(modifier = Modifier.width(150.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        TvClickableSurface(
            onClick = { onTitleClick(title) },
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
            modifier = Modifier.width(150.dp).height(220.dp),
        ) {
            AsyncImage(
                model = title.posterUrl,
                contentDescription = title.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(title.title, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ClipboxDetailScreen(
    id: Long,
    type: ClipboxMediaType,
    onBack: () -> Unit,
    scaffold: CatalogScaffoldContent,
    viewModel: ClipboxDetailViewModel = hiltViewModel(),
) {
    BackHandler(onBack = onBack)
    LaunchedEffect(id, type) { viewModel.open(id, type) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val destination = if (type == ClipboxMediaType.MOVIE) AppDestination.MovieDetail(id)
    else AppDestination.SeriesDetail(id)
    scaffold(destination, state.details?.title?.title ?: "פרטים", null, CatalogNavigationChrome.TopBar, true, true, false) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "back") { TvButton(onClick = onBack) { Text("חזרה") } }
            state.details?.let { details ->
                item(key = "summary") {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        AsyncImage(
                            model = details.title.posterUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.width(180.dp).height(270.dp),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(details.title.title, style = MaterialTheme.typography.headlineLarge)
                            Text(details.title.releaseDate)
                            Text(details.genres.joinToString(" • "))
                            Text(details.title.overview, style = MaterialTheme.typography.bodyLarge)
                            if (details.cast.isNotEmpty()) Text("משתתפים: ${details.cast.joinToString(", ")}")
                        }
                    }
                }
                if (type == ClipboxMediaType.SERIES) {
                    item(key = "seasons") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("עונות", style = MaterialTheme.typography.headlineSmall)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(details.seasons, key = { it.number }) { season ->
                                    TvButton(onClick = { viewModel.selectSeason(season.number) }) {
                                        Text("${season.title} (${season.episodeCount})")
                                    }
                                }
                            }
                        }
                    }
                    item(key = "episode_heading") {
                        Text("פרקים", style = MaterialTheme.typography.headlineSmall)
                    }
                    items(state.episodes, key = { it.number }) { episode ->
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                AsyncImage(
                                    model = episode.stillUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.width(150.dp).height(84.dp),
                                )
                                Column {
                                    Text("${episode.number}. ${episode.title}", style = MaterialTheme.typography.titleMedium)
                                    Text(episode.overview, maxLines = 2)
                                }
                        }
                    }
                }
            }
            if (state.loading) item(key = "loading") { Text("טוען פרטים…") }
            state.error?.let { item(key = "error") { Text(it) } }
        }
    }
}
