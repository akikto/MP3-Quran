package com.example.data

import androidx.room.withTransaction
import com.example.data.entity.BookmarkedAyahEntity
import com.example.data.entity.FavoriteSurahEntity
import com.example.data.entity.PlaylistEntity
import com.example.data.entity.PlaylistItemEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Portable, versioned backup of personal collections only. Playback history, downloads and
 * built-in playlists are deliberately not transferred between devices.
 */
class CollectionsBackup(private val database: AppDatabase) {
    private val dao = database.quranDao()

    suspend fun export(output: OutputStream) {
        val json = database.withTransaction {
            JSONObject().put("version", 1)
                .put("favorites", JSONArray().apply {
                    dao.snapshotFavorites().forEach { put(JSONObject()
                        .put("surahNumber", it.surahNumber).put("addedAt", it.addedAt)) }
                })
                .put("bookmarks", JSONArray().apply {
                    dao.snapshotBookmarks().forEach { put(JSONObject()
                        .put("surahNumber", it.surahNumber).put("ayahNumber", it.ayahNumber)
                        .put("note", it.note).put("timestampMs", it.timestampMs)
                        .put("bookmarkedAt", it.bookmarkedAt)) }
                })
                .put("playlists", JSONArray().apply {
                    dao.snapshotCustomPlaylists().forEach { playlist ->
                        put(JSONObject().put("name", playlist.name)
                            .put("description", playlist.description)
                            .put("createdAt", playlist.createdAt)
                            .put("items", JSONArray().apply {
                                dao.snapshotPlaylistItems(playlist.id).forEach { item ->
                                    put(item.surahNumber)
                                }
                            }))
                    }
                })
        }
        output.write(json.toString(2).toByteArray(Charsets.UTF_8))
    }

    suspend fun import(input: InputStream) {
        // Cap untrusted documents before parsing. No database writes occur until validation ends.
        val buffer = ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        while (true) {
            val count = input.read(chunk)
            if (count == -1) break
            require(buffer.size() + count <= MAX_BYTES) { "Backup file is too large" }
            buffer.write(chunk, 0, count)
        }
        val bytes = buffer.toByteArray()
        val root = try {
            JSONObject(String(bytes, Charsets.UTF_8))
        } catch (e: Exception) {
            throw IllegalArgumentException("Not a valid collections backup", e)
        }
        require(root.int("version") == 1) { "Unsupported backup version" }
        val favorites = root.array("favorites").objects().map {
            FavoriteSurahEntity(it.int("surahNumber").validSurah(), it.long("addedAt").nonNegative())
        }
        val bookmarks = root.array("bookmarks").objects().map {
            BookmarkedAyahEntity(
                surahNumber = it.int("surahNumber").validSurah(),
                ayahNumber = it.int("ayahNumber").also { n -> require(n in 1..300) { "Invalid ayah number" } },
                note = it.string("note"), timestampMs = it.long("timestampMs").nonNegative(),
                bookmarkedAt = it.long("bookmarkedAt").nonNegative()
            )
        }
        val playlists = root.array("playlists").objects().map {
            val name = it.string("name")
            require(name.isNotBlank()) { "Playlist name cannot be empty" }
            val description = it.string("description")
            val createdAt = it.long("createdAt").nonNegative()
            val items = it.array("items").let { array ->
                (0 until array.length()).map { index ->
                    val number = array.opt(index)
                    require(number is Int) { "Invalid playlist item" }
                    number.validSurah()
                }
            }
            require(items.size == items.distinct().size) { "Duplicate playlist item" }
            BackupPlaylist(name, description, createdAt, items)
        }
        database.withTransaction {
            val existingFavorites = dao.snapshotFavorites().map { it.surahNumber }.toSet()
            favorites.filter { it.surahNumber !in existingFavorites }
                .distinctBy { it.surahNumber }.forEach { dao.importFavorite(it) }

            val existingBookmarks = dao.snapshotBookmarks().map { it.identity() }.toMutableSet()
            bookmarks.forEach { bookmark ->
                if (existingBookmarks.add(bookmark.identity())) dao.importBookmark(bookmark)
            }

            val existingPlaylists = dao.snapshotCustomPlaylists().toMutableList()
            val usedPlaylistIds = mutableSetOf<Long>()
            playlists.forEach { saved ->
                val key = saved.name to saved.description
                val candidates = existingPlaylists.filter {
                    (it.name to it.description) == key && it.id !in usedPlaylistIds
                }
                // Multiple playlists may legitimately share a title and description. Match each
                // exported occurrence to at most one existing row, comparing its contents too.
                val exact = candidates.firstOrNull { candidate ->
                    candidate.createdAt == saved.createdAt &&
                        dao.snapshotPlaylistItems(candidate.id).map { it.surahNumber }.toSet() ==
                        saved.items.toSet()
                }
                val onlySavedWithThisTitle = playlists.count { (it.name to it.description) == key } == 1
                val onlyExistingWithThisTitle = existingPlaylists.count {
                    (it.name to it.description) == key
                } == 1
                // For a single playlist of this title, append missing entries to an existing
                // collection instead of creating a second one. Never do this when ambiguous.
                val match = exact ?: candidates.singleOrNull()
                    ?.takeIf { onlySavedWithThisTitle && onlyExistingWithThisTitle }
                val playlistId = match?.id ?: dao.importPlaylist(
                    PlaylistEntity(name = saved.name, description = saved.description,
                        createdAt = saved.createdAt)
                ).also { id ->
                    existingPlaylists.add(PlaylistEntity(id = id, name = saved.name,
                        description = saved.description, createdAt = saved.createdAt))
                }
                usedPlaylistIds.add(playlistId)
                val existingItems = dao.snapshotPlaylistItems(playlistId)
                val numbers = existingItems.map { it.surahNumber }.toMutableSet()
                var nextIndex = (existingItems.maxOfOrNull { it.orderIndex } ?: -1) + 1
                saved.items.forEach { number ->
                    if (numbers.add(number)) {
                        dao.importPlaylistItem(PlaylistItemEntity(playlistId, number, nextIndex++))
                    }
                }
            }
        }
    }

    private data class BackupPlaylist(
        val name: String, val description: String, val createdAt: Long, val items: List<Int>
    )

    private fun BookmarkedAyahEntity.identity() =
        listOf(surahNumber, ayahNumber, note, timestampMs)

    private fun Int.validSurah(): Int = also { require(it in 1..114) { "Invalid surah number" } }
    private fun Long.nonNegative(): Long = also { require(it >= 0) { "Invalid timestamp" } }

    private fun JSONObject.array(key: String): JSONArray =
        requireNotNull(optJSONArray(key)) { "Missing or invalid $key" }

    private fun JSONObject.string(key: String): String {
        val value = opt(key)
        require(value is String) { "Missing or invalid $key" }
        return value
    }

    private fun JSONObject.int(key: String): Int {
        val value = opt(key)
        require(value is Int) { "Missing or invalid $key" }
        return value
    }

    private fun JSONObject.long(key: String): Long {
        val value = opt(key)
        require(value is Number && value !is Double && value !is Float) { "Missing or invalid $key" }
        return value.toLong()
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { index ->
        val value = opt(index)
        require(value is JSONObject) { "Invalid collection entry" }
        value
    }

    companion object {
        private const val MAX_BYTES = 2 * 1024 * 1024
    }
}