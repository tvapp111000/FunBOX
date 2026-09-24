package com.streamvault.data.remote.clipbox

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class ClipboxSavedTitle(
    val title: ClipboxTitle,
    val favorite: Boolean,
    val watchlist: Boolean,
)

/** Guest library state. Account synchronization can be added after the account API is verified. */
@Singleton
class ClipboxUserStateRepository @Inject constructor(@ApplicationContext context: Context) {
    private val preferences = context.getSharedPreferences("clipbox_guest_library", Context.MODE_PRIVATE)
    private val _items = MutableStateFlow(readItems())
    val items = _items.asStateFlow()

    @Synchronized
    fun toggleFavorite(title: ClipboxTitle) = update(title) { it.copy(favorite = !it.favorite) }

    @Synchronized
    fun toggleWatchlist(title: ClipboxTitle) = update(title) { it.copy(watchlist = !it.watchlist) }

    private fun update(title: ClipboxTitle, change: (ClipboxSavedTitle) -> ClipboxSavedTitle) {
        val current = _items.value
        val old = current.firstOrNull { it.title.id == title.id && it.title.type == title.type }
            ?: ClipboxSavedTitle(title, favorite = false, watchlist = false)
        val changed = change(old.copy(title = title))
        val next = current.filterNot { it.title.id == title.id && it.title.type == title.type } +
            listOfNotNull(changed.takeIf { it.favorite || it.watchlist })
        _items.value = next
        val serialized = JSONArray()
        next.forEach { item ->
            serialized.put(JSONObject().apply {
                put("id", item.title.id)
                put("type", item.title.type.name)
                put("title", item.title.title)
                put("overview", item.title.overview)
                put("poster", item.title.posterUrl)
                put("backdrop", item.title.backdropUrl)
                put("releaseDate", item.title.releaseDate)
                put("rating", item.title.rating)
                put("favorite", item.favorite)
                put("watchlist", item.watchlist)
            })
        }
        preferences.edit().putString("items", serialized.toString()).apply()
    }

    private fun readItems(): List<ClipboxSavedTitle> = runCatching {
        val array = JSONArray(preferences.getString("items", "[]"))
        (0 until array.length()).mapNotNull { index ->
            val json = array.optJSONObject(index) ?: return@mapNotNull null
            val id = json.optLong("id")
            val type = ClipboxMediaType.entries.firstOrNull { it.name == json.optString("type") }
                ?: return@mapNotNull null
            if (id <= 0L) return@mapNotNull null
            ClipboxSavedTitle(
                title = ClipboxTitle(
                    id = id,
                    type = type,
                    title = json.optString("title"),
                    overview = json.optString("overview"),
                    posterUrl = json.optString("poster").takeIf { it.startsWith("https://") },
                    backdropUrl = json.optString("backdrop").takeIf { it.startsWith("https://") },
                    releaseDate = json.optString("releaseDate"),
                    rating = json.optDouble("rating"),
                ),
                favorite = json.optBoolean("favorite"),
                watchlist = json.optBoolean("watchlist"),
            )
        }
    }.getOrDefault(emptyList())
}
