package com.subaruplayer.app.data

import com.subaruplayer.app.util.NaturalOrder
import com.subaruplayer.app.util.pathLabel
import kotlinx.serialization.Serializable

/** Um vídeo do aparelho, do jeito que o MediaStore informa (inclusive a pasta onde ele está). */
data class Video(
    val id: Long,
    val uri: String,
    val name: String,
    /** Pasta do vídeo relativa ao armazenamento, sempre terminando em "/" (ex.: "Movies/Subaru DL/Minha Playlist/"). */
    val relPath: String,
    val durationMs: Long,
    val size: Long,
    val dateAdded: Long,
    val width: Int,
    val height: Int,
) {
    /** Identidade estável do vídeo (o id do MediaStore pode mudar se o arquivo for reindexado). */
    val key: String get() = relPath + name
    val title: String get() = name.substringBeforeLast('.')
    val folderLabel: String get() = pathLabel(relPath)
}

/** Uma pasta da árvore de vídeos. A árvore espelha exatamente as pastas do aparelho. */
class FolderNode(val name: String, val path: String) {
    val folders = mutableListOf<FolderNode>()
    val videos = mutableListOf<Video>()

    /** Quantos vídeos existem aqui dentro, contando as subpastas. */
    var total: Int = 0
        internal set
}

class Library(val videos: List<Video>, val root: FolderNode, private val folderMap: Map<String, FolderNode>) {
    val byKey: Map<String, Video> = videos.associateBy { it.key }

    fun folder(path: String): FolderNode? = folderMap[path]

    /** Procura em todas as pastas, pelo nome do vídeo ou pelo caminho. */
    fun search(query: String): List<Video> {
        val words = query.trim().lowercase().split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()
        return videos.filter { v ->
            val hay = (v.name + " " + v.relPath).lowercase()
            words.all { it in hay }
        }.sortedWith(compareBy(NaturalOrder) { it.relPath + it.name })
    }

    companion object {
        val EMPTY = build(emptyList())

        fun build(videos: List<Video>): Library {
            val root = FolderNode("", "")
            val map = HashMap<String, FolderNode>()
            map[""] = root

            fun node(path: String): FolderNode {
                map[path]?.let { return it }
                val trimmed = path.trimEnd('/')
                val parentPath = trimmed.substringBeforeLast('/', "").let { if (it.isEmpty()) "" else "$it/" }
                val n = FolderNode(trimmed.substringAfterLast('/'), path)
                map[path] = n
                node(parentPath).folders.add(n)
                return n
            }

            for (v in videos) node(v.relPath).videos.add(v)

            fun finish(n: FolderNode): Int {
                n.folders.sortWith(compareBy(NaturalOrder) { it.name })
                var t = n.videos.size
                for (c in n.folders) t += finish(c)
                n.total = t
                return t
            }
            finish(root)
            return Library(videos, root, map)
        }
    }
}

// ---------- Dados do usuário (favoritos, histórico, playlists, marcadores) ----------

@Serializable
data class HistoryEntry(
    val key: String,
    val title: String,
    val relPath: String,
    val positionMs: Long,
    val durationMs: Long,
    val playedAt: Long,
)

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    /** Os vídeos são guardados pela chave (pasta + nome), na ordem da playlist. */
    val keys: List<String> = emptyList(),
    val createdAt: Long = 0,
)

@Serializable
data class Bookmark(val timeMs: Long, val label: String)

@Serializable
data class UserData(
    val favorites: List<String> = emptyList(),
    /** Mais recente primeiro. */
    val history: List<HistoryEntry> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val bookmarks: Map<String, List<Bookmark>> = emptyMap(),
)

/** De onde veio a reprodução: define qual é a fila (o "próximo" e o "anterior"). */
data class Queue(
    val items: List<Video>,
    val index: Int,
    /** Texto mostrado no player: "Pasta Minha Playlist", "Favoritos"… */
    val label: String,
) {
    val current: Video get() = items[index]
    val hasPrev: Boolean get() = index > 0
    val hasNext: Boolean get() = index < items.lastIndex
}
