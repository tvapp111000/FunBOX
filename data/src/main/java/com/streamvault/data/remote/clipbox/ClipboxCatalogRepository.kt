package com.streamvault.data.remote.clipbox

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

enum class ClipboxMediaType { MOVIE, SERIES }

data class ClipboxTitle(
    val id: Long,
    val type: ClipboxMediaType,
    val title: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val releaseDate: String,
    val rating: Double,
)

data class ClipboxShelf(val title: String, val items: List<ClipboxTitle>)

data class ClipboxSeason(val number: Int, val title: String, val episodeCount: Int, val posterUrl: String?)

data class ClipboxEpisode(
    val number: Int,
    val title: String,
    val overview: String,
    val stillUrl: String?,
    val airDate: String,
)

data class ClipboxDetails(
    val title: ClipboxTitle,
    val genres: List<String>,
    val cast: List<String>,
    val seasons: List<ClipboxSeason>,
)

/** Clipbox's catalog contract: its signed config supplies the key for official TMDB v3 calls. */
@Singleton
class ClipboxCatalogRepository @Inject constructor(private val clipboxApi: ClipboxApi) {
    private val client = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .build()
    private val keyMutex = Mutex()
    @Volatile private var cachedKey: Pair<String, Long>? = null
    private val pageCache = ConcurrentHashMap<String, Pair<Long, List<ClipboxTitle>>>()

    suspend fun home(): List<ClipboxShelf> = coroutineScope {
        val trending = async { trending() }
        val movies = async { movies() }
        val series = async { series() }
        listOf(
            ClipboxShelf("מומלצים השבוע", trending.await()),
            ClipboxShelf("סרטים פופולריים", movies.await()),
            ClipboxShelf("סדרות פופולריות", series.await()),
        )
    }

    suspend fun trending(page: Int = 1): List<ClipboxTitle> = list(
        "/trending/all/week", page, ClipboxMediaType.MOVIE,
    )

    suspend fun movies(page: Int = 1): List<ClipboxTitle> = list(
        "/discover/movie", page, ClipboxMediaType.MOVIE,
        "sort_by" to "popularity.desc", "with_original_language" to "en|nl",
    )

    suspend fun series(page: Int = 1): List<ClipboxTitle> = list(
        "/discover/tv", page, ClipboxMediaType.SERIES,
        "sort_by" to "popularity.desc", "with_original_language" to "en|nl",
    )

    suspend fun search(query: String, page: Int = 1): List<ClipboxTitle> {
        if (query.isBlank()) return emptyList()
        return list("/search/multi", page, ClipboxMediaType.MOVIE, "query" to query.trim())
    }

    suspend fun movieDetails(id: Long): ClipboxDetails = details("/movie/$id", ClipboxMediaType.MOVIE)
    suspend fun seriesDetails(id: Long): ClipboxDetails = details("/tv/$id", ClipboxMediaType.SERIES)

    suspend fun episodes(seriesId: Long, seasonNumber: Int): List<ClipboxEpisode> {
        require(seriesId > 0 && seasonNumber >= 0)
        val json = get("/tv/$seriesId/season/$seasonNumber")
        return json.optJSONArray("episodes").objects().mapNotNull { item ->
            val number = item.optInt("episode_number", -1)
            if (number < 0) null else ClipboxEpisode(
                number = number,
                title = item.optString("name"),
                overview = item.optString("overview"),
                stillUrl = image(item.optString("still_path"), "w500"),
                airDate = item.optString("air_date"),
            )
        }
    }

    suspend fun episodeDetails(seriesId: Long, seasonNumber: Int, episodeNumber: Int): ClipboxEpisode {
        require(seriesId > 0 && seasonNumber >= 0 && episodeNumber > 0)
        val item = get("/tv/$seriesId/season/$seasonNumber/episode/$episodeNumber")
        return ClipboxEpisode(
            number = item.optInt("episode_number", episodeNumber),
            title = item.optString("name"),
            overview = item.optString("overview"),
            stillUrl = image(item.optString("still_path"), "w500"),
            airDate = item.optString("air_date"),
        )
    }

    private suspend fun details(path: String, type: ClipboxMediaType): ClipboxDetails {
        val json = get(path, "append_to_response" to "credits,videos,images")
        val title = json.title(type) ?: error("Clipbox catalog detail has no title")
        val cast = json.optJSONObject("credits")?.optJSONArray("cast").objects()
            .take(12).mapNotNull { it.optString("name").takeIf(String::isNotBlank) }
        val genres = json.optJSONArray("genres").objects()
            .mapNotNull { it.optString("name").takeIf(String::isNotBlank) }
        val seasons = json.optJSONArray("seasons").objects().mapNotNull { season ->
            val number = season.optInt("season_number", -1)
            if (number < 0) null else ClipboxSeason(
                number = number,
                title = season.optString("name"),
                episodeCount = season.optInt("episode_count", 0),
                posterUrl = image(season.optString("poster_path"), "w500"),
            )
        }
        return ClipboxDetails(title, genres, cast, seasons)
    }

    private suspend fun list(
        path: String,
        page: Int,
        defaultType: ClipboxMediaType,
        vararg extra: Pair<String, String>,
    ): List<ClipboxTitle> {
        require(page in 1..500)
        val cacheId = "$path:$page:${extra.contentHashCode()}"
        pageCache[cacheId]?.takeIf { System.currentTimeMillis() - it.first < CACHE_MILLIS }
            ?.let { return it.second }
        val result = get(path, "page" to page.toString(), *extra)
            .optJSONArray("results").objects()
            .mapNotNull { it.title(defaultType) }
            .filterNot { it.overview.isBlank() }
        pageCache[cacheId] = System.currentTimeMillis() to result
        return result
    }

    private suspend fun get(path: String, vararg params: Pair<String, String>): JSONObject =
        withContext(Dispatchers.IO) {
            val key = catalogKey()
            val builder = HttpUrl.Builder().scheme("https").host("api.themoviedb.org")
                .addPathSegment("3")
            path.trimStart('/').split('/').forEach(builder::addPathSegment)
            builder.addQueryParameter("api_key", key).addQueryParameter("language", "he-IL")
            params.forEach { (name, value) -> builder.addQueryParameter(name, value) }
            val request = Request.Builder().url(builder.build())
                .header("Accept", "application/json").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Clipbox catalog HTTP ${response.code}")
                JSONObject(response.body?.string().orEmpty())
            }
        }

    private suspend fun catalogKey(): String {
        cachedKey?.takeIf { System.currentTimeMillis() < it.second }?.let { return it.first }
        return keyMutex.withLock {
            cachedKey?.takeIf { System.currentTimeMillis() < it.second }?.first ?: run {
                val key = clipboxApi.fetchConfig().tmdbKey
                check(key.isNotBlank()) { "Clipbox catalog is unavailable" }
                cachedKey = key to (System.currentTimeMillis() + CACHE_MILLIS)
                key
            }
        }
    }

    private fun JSONObject.title(defaultType: ClipboxMediaType): ClipboxTitle? {
        if (optBoolean("adult", false)) return null
        val id = optLong("id", -1L)
        if (id <= 0L) return null
        val type = when (optString("media_type")) {
            "movie" -> ClipboxMediaType.MOVIE
            "tv" -> ClipboxMediaType.SERIES
            "person" -> return null
            else -> defaultType
        }
        val name = optString(if (type == ClipboxMediaType.MOVIE) "title" else "name")
        if (name.isBlank()) return null
        return ClipboxTitle(
            id = id,
            type = type,
            title = name,
            overview = optString("overview"),
            posterUrl = image(optString("poster_path"), "w500"),
            backdropUrl = image(optString("backdrop_path"), "w1280"),
            releaseDate = optString(if (type == ClipboxMediaType.MOVIE) "release_date" else "first_air_date"),
            rating = optDouble("vote_average", 0.0),
        )
    }

    private fun image(path: String, size: String): String? =
        path.takeIf { it.startsWith("/") }?.let { "https://image.tmdb.org/t/p/$size$it" }

    private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else
        (0 until length()).mapNotNull(::optJSONObject)

    private companion object { const val CACHE_MILLIS = 30 * 60 * 1000L }
}
