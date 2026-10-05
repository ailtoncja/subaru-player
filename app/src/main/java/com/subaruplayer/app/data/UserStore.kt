package com.subaruplayer.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

/**
 * Favoritos, histórico (com a posição onde cada vídeo parou), playlists e marcadores.
 * Tudo num único JSON em filesDir, gravado por uma thread própria (escreve num arquivo temporário e renomeia,
 * então uma queda no meio da gravação nunca deixa o arquivo pela metade).
 */
object UserStore {
    private const val MAX_HISTORY = 300
    private lateinit var file: File
    private val _state = MutableStateFlow(UserData())
    val state: StateFlow<UserData> = _state.asStateFlow()
    val value get() = _state.value
    private val writer = Executors.newSingleThreadExecutor()

    fun init(ctx: Context) {
        file = File(ctx.applicationContext.filesDir, "userdata.json")
        if (file.exists()) {
            _state.value = runCatching { AppJson.decodeFromString<UserData>(file.readText()) }.getOrDefault(UserData())
        }
    }

    @Synchronized
    private fun update(block: (UserData) -> UserData) {
        val novo = block(_state.value)
        if (novo == _state.value) return
        _state.value = novo
        val json = AppJson.encodeToString(UserData.serializer(), novo)
        writer.execute {
            runCatching {
                val tmp = File(file.parentFile, file.name + ".tmp")
                tmp.writeText(json)
                if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
            }
        }
    }

    // ---------- Favoritos ----------

    fun isFavorite(key: String) = key in value.favorites

    fun toggleFavorite(key: String) = update {
        it.copy(favorites = if (key in it.favorites) it.favorites - key else it.favorites + key)
    }

    // ---------- Histórico e retomada ----------

    /** Guarda onde o vídeo parou. Perto do fim (ou assistido até o fim) a posição volta a zero. */
    fun saveProgress(video: Video, positionMs: Long, durationMs: Long) = update { d ->
        val dur = if (durationMs > 0) durationMs else video.durationMs
        val finished = dur > 0 && positionMs >= dur - 5000
        val entry = HistoryEntry(
            key = video.key, title = video.title, relPath = video.relPath,
            positionMs = if (finished) 0 else positionMs, durationMs = dur, playedAt = System.currentTimeMillis(),
        )
        d.copy(history = (listOf(entry) + d.history.filterNot { it.key == video.key }).take(MAX_HISTORY))
    }

    /** Onde retomar: só vale se o vídeo parou depois dos primeiros 10 s e antes do fim. */
    fun resumePosition(key: String): Long {
        val e = value.history.firstOrNull { it.key == key } ?: return 0
        return if (e.positionMs > 10_000 && (e.durationMs <= 0 || e.positionMs < e.durationMs - 5000)) e.positionMs else 0
    }

    fun removeFromHistory(key: String) = update { d -> d.copy(history = d.history.filterNot { it.key == key }) }
    fun clearHistory() = update { it.copy(history = emptyList()) }

    // ---------- Playlists ----------

    fun createPlaylist(name: String, keys: List<String> = emptyList()): String {
        val id = UUID.randomUUID().toString()
        update { it.copy(playlists = it.playlists + Playlist(id, name.trim().ifEmpty { "Nova playlist" }, keys.distinct(), System.currentTimeMillis())) }
        return id
    }

    fun renamePlaylist(id: String, name: String) = update { d ->
        d.copy(playlists = d.playlists.map { if (it.id == id) it.copy(name = name.trim().ifEmpty { it.name }) else it })
    }

    fun deletePlaylist(id: String) = update { d -> d.copy(playlists = d.playlists.filterNot { it.id == id }) }

    fun addToPlaylist(id: String, key: String) = update { d ->
        d.copy(playlists = d.playlists.map { if (it.id == id && key !in it.keys) it.copy(keys = it.keys + key) else it })
    }

    fun removeFromPlaylist(id: String, key: String) = update { d ->
        d.copy(playlists = d.playlists.map { if (it.id == id) it.copy(keys = it.keys - key) else it })
    }

    fun movePlaylistItem(id: String, from: Int, to: Int) = update { d ->
        d.copy(playlists = d.playlists.map { p ->
            if (p.id != id || from !in p.keys.indices || to !in p.keys.indices) p
            else p.copy(keys = p.keys.toMutableList().apply { add(to, removeAt(from)) })
        })
    }

    // ---------- Marcadores ----------

    fun addBookmark(key: String, timeMs: Long, label: String) = update { d ->
        val list = (d.bookmarks[key].orEmpty() + Bookmark(timeMs, label)).sortedBy { it.timeMs }
        d.copy(bookmarks = d.bookmarks + (key to list))
    }

    fun removeBookmark(key: String, timeMs: Long) = update { d ->
        d.copy(bookmarks = d.bookmarks + (key to d.bookmarks[key].orEmpty().filterNot { it.timeMs == timeMs }))
    }
}
