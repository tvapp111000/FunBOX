package com.streamvault.feature.catalog.presentation.clipbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamvault.data.remote.clipbox.ClipboxCatalogRepository
import com.streamvault.data.remote.clipbox.ClipboxDetails
import com.streamvault.data.remote.clipbox.ClipboxEpisode
import com.streamvault.data.remote.clipbox.ClipboxMediaType
import com.streamvault.data.remote.clipbox.ClipboxShelf
import com.streamvault.data.remote.clipbox.ClipboxTitle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ClipboxBrowseKind { HOME, MOVIES, SERIES }

data class ClipboxBrowseState(
    val kind: ClipboxBrowseKind? = null,
    val shelves: List<ClipboxShelf> = emptyList(),
    val titles: List<ClipboxTitle> = emptyList(),
    val page: Int = 0,
    val loading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ClipboxBrowseViewModel @Inject constructor(
    private val repository: ClipboxCatalogRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ClipboxBrowseState())
    val state = _state.asStateFlow()

    fun open(kind: ClipboxBrowseKind) {
        if (_state.value.kind == kind && (_state.value.titles.isNotEmpty() || _state.value.shelves.isNotEmpty())) return
        _state.value = ClipboxBrowseState(kind = kind)
        loadMore()
    }

    fun loadMore() {
        val current = _state.value
        if (current.loading || current.kind == null || current.kind == ClipboxBrowseKind.HOME && current.page > 0) return
        val nextPage = current.page + 1
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                when (current.kind) {
                    ClipboxBrowseKind.HOME -> repository.home() to emptyList<ClipboxTitle>()
                    ClipboxBrowseKind.MOVIES -> emptyList<ClipboxShelf>() to repository.movies(nextPage)
                    ClipboxBrowseKind.SERIES -> emptyList<ClipboxShelf>() to repository.series(nextPage)
                    null -> error("Clipbox screen type is missing")
                }
            }.onSuccess { (shelves, titles) ->
                _state.update {
                    it.copy(
                        shelves = if (current.kind == ClipboxBrowseKind.HOME) shelves else it.shelves,
                        titles = (it.titles + titles).distinctBy(ClipboxTitle::id),
                        page = nextPage,
                        loading = false,
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(loading = false, error = error.message ?: "טעינת הקטלוג נכשלה") }
            }
        }
    }
}

data class ClipboxDetailState(
    val details: ClipboxDetails? = null,
    val seasonNumber: Int? = null,
    val episodes: List<ClipboxEpisode> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ClipboxDetailViewModel @Inject constructor(
    private val repository: ClipboxCatalogRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ClipboxDetailState())
    val state = _state.asStateFlow()

    fun open(id: Long, type: ClipboxMediaType) {
        if (_state.value.details?.title?.id == id && _state.value.details?.title?.type == type) return
        _state.value = ClipboxDetailState(loading = true)
        viewModelScope.launch {
            runCatching {
                if (type == ClipboxMediaType.MOVIE) repository.movieDetails(id)
                else repository.seriesDetails(id)
            }.onSuccess { details ->
                _state.update { it.copy(details = details, loading = false) }
                details.seasons.firstOrNull { it.number > 0 }?.let { selectSeason(it.number) }
            }.onFailure { error ->
                _state.update { it.copy(loading = false, error = error.message ?: "טעינת פרטי התוכן נכשלה") }
            }
        }
    }

    fun selectSeason(number: Int) {
        val details = _state.value.details ?: return
        if (details.title.type != ClipboxMediaType.SERIES) return
        _state.update { it.copy(seasonNumber = number, episodes = emptyList(), loading = true) }
        viewModelScope.launch {
            runCatching { repository.episodes(details.title.id, number) }
                .onSuccess { episodes -> _state.update { it.copy(episodes = episodes, loading = false) } }
                .onFailure { error -> _state.update { it.copy(loading = false, error = error.message ?: "טעינת הפרקים נכשלה") } }
        }
    }
}
