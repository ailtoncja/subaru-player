package com.subaruplayer.app

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.subaruplayer.app.data.FolderNode
import com.subaruplayer.app.data.HwMode
import com.subaruplayer.app.data.Library
import com.subaruplayer.app.data.MediaRepository
import com.subaruplayer.app.data.Queue
import com.subaruplayer.app.data.SettingsStore
import com.subaruplayer.app.data.SortBy
import com.subaruplayer.app.data.UserStore
import com.subaruplayer.app.data.Video
import com.subaruplayer.app.player.PlayerEngine
import com.subaruplayer.app.util.NaturalOrder
import com.subaruplayer.app.util.formatTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class Tab(val label: String) {
    LIBRARY("Biblioteca"), PLAYLISTS("Playlists"), HISTORY("Histórico"), SETTINGS("Ajustes"),
}

/** Id usado no lugar do id de uma playlist para abrir a lista de Favoritos. */
const val FAVORITES_ID = "favoritos"

class AppViewModel(private val app: Application) : AndroidViewModel(app) {
    val engine = PlayerEngine(app)

    // ---------- Permissão e biblioteca ----------
    var hasPermission by mutableStateOf(MediaRepository.hasPermission(app))
        private set
    var library by mutableStateOf(Library.EMPTY)
        private set
    var loading by mutableStateOf(false)
        private set
    var loadedOnce by mutableStateOf(false)
        private set
    private var stopObserving: (() -> Unit)? = null

    // ---------- Navegação ----------
    var tab by mutableStateOf(Tab.LIBRARY)
    /** Pasta aberta na biblioteca ("" = início). Sempre um caminho real do aparelho. */
    var folderPath by mutableStateOf("")
    var searchOpen by mutableStateOf(false)
    var searchQuery by mutableStateOf("")
    /** Playlist aberta na aba Playlists (null = a lista de playlists; [FAVORITES_ID] = favoritos). */
    var openPlaylistId by mutableStateOf<String?>(null)

    // ---------- Reprodução ----------
    /** Enquanto houver fila, o player está aberto. */
    var queue by mutableStateOf<Queue?>(null)
        private set

    val toasts = MutableSharedFlow<String>(extraBufferCapacity = 4)

    /** Posição de rolagem de cada pasta: ao voltar do player a lista fica onde estava. */
    private val gridStates = HashMap<String, LazyGridState>()
    fun gridState(path: String): LazyGridState = gridStates.getOrPut(path) { LazyGridState() }

    init {
        UserStore.state // garante que o armazenamento já foi iniciado pela Application
        engine.onEnded = { onVideoEnded() }
        if (hasPermission) startLibrary()
        // Guarda a posição a cada poucos segundos (é daqui que vem o "retomar de onde parou")
        viewModelScope.launch {
            while (true) {
                delay(5000)
                if (queue != null && engine.state.value.playing) saveProgressNow()
            }
        }
    }

    fun toast(msg: String) { toasts.tryEmit(msg) }

    // ---------- Biblioteca ----------

    private fun startLibrary() {
        refresh()
        if (stopObserving == null) stopObserving = MediaRepository.observe(app) { refresh() }
    }

    fun onPermissionResult() {
        hasPermission = MediaRepository.hasPermission(app)
        if (hasPermission) startLibrary()
    }

    fun refresh() {
        if (!MediaRepository.hasPermission(app)) { hasPermission = false; return }
        hasPermission = true
        viewModelScope.launch {
            loading = true
            val videos = withContext(Dispatchers.IO) { MediaRepository.load(app) }
            library = withContext(Dispatchers.Default) { Library.build(videos) }
            // Se a pasta aberta sumiu (arquivos apagados), sobe até uma que ainda exista
            while (folderPath.isNotEmpty() && library.folder(folderPath) == null) folderPath = parentOf(folderPath)
            loading = false
            loadedOnce = true
        }
    }

    fun openFolder(path: String) { folderPath = path; searchOpen = false }
    fun upFolder() { folderPath = parentOf(folderPath) }

    private fun parentOf(path: String): String {
        val p = path.trimEnd('/').substringBeforeLast('/', "")
        return if (p.isEmpty()) "" else "$p/"
    }

    /** Vídeos de uma pasta na ordem escolhida (por padrão, a ordem natural do nome: 1, 2, 10…). */
    fun sortedVideos(node: FolderNode): List<Video> {
        val s = SettingsStore.value
        val cmp: Comparator<Video> = when (s.sortBy) {
            SortBy.NAME -> compareBy(NaturalOrder) { it.name }
            SortBy.DATE -> compareBy { it.dateAdded }
            SortBy.DURATION -> compareBy { it.durationMs }
            SortBy.SIZE -> compareBy { it.size }
        }
        return node.videos.sortedWith(if (s.sortDesc) cmp.reversed() else cmp)
    }

    // ---------- Fila e player ----------

    /** Toca [video] com a fila sendo a pasta onde ele está, na mesma ordem em que a pasta é mostrada. */
    fun playInFolder(video: Video) {
        val node = library.folder(video.relPath)
        val items = node?.let { sortedVideos(it) }?.takeIf { list -> list.any { it.key == video.key } } ?: listOf(video)
        val label = if (node != null && node.name.isNotEmpty()) node.name else "Armazenamento interno"
        startQueue(items, items.indexOfFirst { it.key == video.key }.coerceAtLeast(0), label)
    }

    fun playFolder(node: FolderNode) {
        val items = sortedVideos(node)
        if (items.isEmpty()) return
        startQueue(items, 0, node.name.ifEmpty { "Armazenamento interno" })
    }

    fun startQueue(items: List<Video>, index: Int, label: String) {
        if (items.isEmpty()) return
        queue = Queue(items, index.coerceIn(0, items.lastIndex), label)
        loadCurrent()
    }

    private fun loadCurrent() {
        val q = queue ?: return
        val v = q.current
        val start = if (SettingsStore.value.resume) UserStore.resumePosition(v.key) else 0L
        engine.load(v, start, SettingsStore.value)
        if (start > 0) toast("Retomando de ${formatTime(start)}")
        UserStore.saveProgress(v, start, v.durationMs)
    }

    /** Tenta de novo o vídeo atual; [software] força a decodificação por software só desta vez. */
    fun retryCurrent(software: Boolean) {
        val q = queue ?: return
        val s = SettingsStore.value
        val start = engine.state.value.positionMs.takeIf { it > 1000 } ?: UserStore.resumePosition(q.current.key)
        engine.load(q.current, start, if (software) s.copy(hwMode = HwMode.SOFTWARE) else s)
        if (software) toast("Tentando por software")
    }

    fun next() {
        val q = queue ?: return
        if (!q.hasNext) return
        saveProgressNow()
        queue = q.copy(index = q.index + 1)
        loadCurrent()
    }

    /** Depois dos primeiros segundos, "anterior" volta ao começo do vídeo; no começo, vai para o vídeo anterior. */
    fun previous() {
        val q = queue ?: return
        if (engine.state.value.positionMs > 3000 || !q.hasPrev) { engine.seekTo(0); return }
        saveProgressNow()
        queue = q.copy(index = q.index - 1)
        loadCurrent()
    }

    fun jumpTo(index: Int) {
        val q = queue ?: return
        if (index !in q.items.indices || index == q.index) return
        saveProgressNow()
        queue = q.copy(index = index)
        loadCurrent()
    }

    fun removeFromQueue(index: Int) {
        val q = queue ?: return
        if (q.items.size <= 1 || index == q.index || index !in q.items.indices) return
        val items = q.items.toMutableList().apply { removeAt(index) }
        queue = q.copy(items = items, index = if (index < q.index) q.index - 1 else q.index)
    }

    fun saveQueueAsPlaylist(name: String) {
        val q = queue ?: return
        UserStore.createPlaylist(name, q.items.map { it.key })
        toast("Playlist “$name” criada")
    }

    private fun onVideoEnded() {
        val q = queue ?: return
        UserStore.saveProgress(q.current, q.current.durationMs, q.current.durationMs)
        if (SettingsStore.value.autoNext && q.hasNext) next()
    }

    fun saveProgressNow() {
        val q = queue ?: return
        val s = engine.state.value
        if (!s.started) return
        UserStore.saveProgress(q.current, s.positionMs, s.durationMs)
    }

    fun closePlayer() {
        saveProgressNow()
        engine.stopAndRelease()
        queue = null
    }

    /** O app foi para o fundo (sem PiP): pausa e guarda onde parou. */
    fun onBackgrounded() {
        if (queue == null) return
        engine.pause()
        saveProgressNow()
    }

    /** Vídeo aberto de fora do app ("Abrir com"). */
    fun openExternal(uri: Uri) {
        val inLibrary = library.videos.firstOrNull { it.uri == uri.toString() }
        if (inLibrary != null) { playInFolder(inLibrary); return }
        val v = MediaRepository.describeExternal(app, uri)
        startQueue(listOf(v), 0, "Aberto de fora do app")
    }

    /** Legenda escolhida no seletor de arquivos: o libVLC só lê arquivos comuns, então copiamos para o cache. */
    fun addSubtitleFile(uri: Uri) {
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                runCatching {
                    var name = "legenda.srt"
                    app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                        if (c.moveToFirst()) c.getString(0)?.let { name = it }
                    }
                    val dir = File(app.cacheDir, "subs").apply { mkdirs() }
                    val out = File(dir, name)
                    app.contentResolver.openInputStream(uri)?.use { input -> out.outputStream().use { input.copyTo(it) } }
                    out
                }.getOrNull()
            }
            if (file != null && file.length() > 0) {
                engine.addSubtitle(Uri.fromFile(file))
                toast("Legenda carregada: ${file.name}")
            } else toast("Não consegui ler esse arquivo de legenda")
        }
    }

    override fun onCleared() {
        stopObserving?.invoke()
        engine.stopAndRelease()
        super.onCleared()
    }
}
