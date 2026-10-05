package com.subaruplayer.app.ui.playlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.subaruplayer.app.AppViewModel
import com.subaruplayer.app.FAVORITES_ID
import com.subaruplayer.app.data.Playlist
import com.subaruplayer.app.data.UserStore
import com.subaruplayer.app.data.Video
import com.subaruplayer.app.ui.components.AddToPlaylistDialog
import com.subaruplayer.app.ui.components.BtnGhost
import com.subaruplayer.app.ui.components.Empty
import com.subaruplayer.app.ui.components.IconTile
import com.subaruplayer.app.ui.components.MenuAction
import com.subaruplayer.app.ui.components.PlayAllButton
import com.subaruplayer.app.ui.components.PromptDialog
import com.subaruplayer.app.ui.components.SCard
import com.subaruplayer.app.ui.components.VideoRow
import com.subaruplayer.app.ui.components.videoDetails
import com.subaruplayer.app.ui.theme.C
import com.subaruplayer.app.ui.theme.T
import com.subaruplayer.app.util.formatTime

@Composable
fun PlaylistsScreen(vm: AppViewModel) {
    val id = vm.openPlaylistId
    if (id == null) PlaylistList(vm) else PlaylistDetail(vm, id)
}

private fun durationOf(videos: List<Video>): String {
    val total = videos.sumOf { it.durationMs }
    return if (total > 0) " · " + formatTime(total) else ""
}

@Composable
private fun PlaylistList(vm: AppViewModel) {
    val user by UserStore.state.collectAsState()
    var creating by remember { mutableStateOf(false) }
    val favorites = remember(user.favorites, vm.library) { user.favorites.mapNotNull { vm.library.byKey[it] } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Suas listas", style = T.headlineSm, modifier = Modifier.weight(1f))
                BtnGhost("Nova playlist", { creating = true }, icon = Icons.AutoMirrored.Outlined.PlaylistAdd)
            }
        }
        item {
            SCard(accent = C.Gold, onClick = { vm.openPlaylistId = FAVORITES_ID }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Filled.Star, tint = C.Gold)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Favoritos", style = T.title)
                        Text(
                            (if (favorites.size == 1) "1 vídeo" else "${favorites.size} vídeos") + durationOf(favorites),
                            style = T.labelMd,
                        )
                    }
                }
            }
        }
        if (user.playlists.isEmpty()) {
            item {
                Empty(
                    "Crie uma playlist e, na lista de vídeos, use o menu ⋮ para adicionar vídeos a ela. Dentro do player também dá para salvar a fila inteira como playlist.",
                    icon = Icons.AutoMirrored.Outlined.PlaylistPlay, title = "Nenhuma playlist ainda",
                )
            }
        }
        itemsIndexed(user.playlists, key = { _, p -> p.id }) { _, p ->
            val videos = p.keys.mapNotNull { vm.library.byKey[it] }
            SCard(onClick = { vm.openPlaylistId = p.id }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.AutoMirrored.Outlined.PlaylistPlay)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.name, style = T.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            (if (p.keys.size == 1) "1 vídeo" else "${p.keys.size} vídeos") + durationOf(videos),
                            style = T.labelMd,
                        )
                    }
                }
            }
        }
    }
    if (creating) PromptDialog("Nova playlist", "", "Criar", { name -> UserStore.createPlaylist(name); creating = false }, { creating = false })
}

@Composable
private fun PlaylistDetail(vm: AppViewModel, id: String) {
    val user by UserStore.state.collectAsState()
    val isFav = id == FAVORITES_ID
    val playlist: Playlist? = if (isFav) null else user.playlists.firstOrNull { it.id == id }
    if (!isFav && playlist == null) { vm.openPlaylistId = null; return }

    val name = if (isFav) "Favoritos" else playlist!!.name
    val keys = if (isFav) user.favorites else playlist!!.keys
    val resolved: List<Pair<String, Video?>> = remember(keys, vm.library) { keys.map { it to vm.library.byKey[it] } }
    val available = resolved.mapNotNull { it.second }

    var menuOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var addKey by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ vm.openPlaylistId = null }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar", tint = C.Text) }
            Column(Modifier.weight(1f)) {
                Text(name, style = T.headlineSm, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text((if (keys.size == 1) "1 vídeo" else "${keys.size} vídeos") + durationOf(available), style = T.labelMd)
            }
            if (!isFav) {
                androidx.compose.foundation.layout.Box {
                    IconButton({ menuOpen = true }) { Icon(Icons.Outlined.MoreVert, "Mais opções", tint = C.Muted) }
                    DropdownMenu(menuOpen, { menuOpen = false }, containerColor = C.CardHigh) {
                        DropdownMenuItem(
                            text = { Text("Renomear", style = T.bodyMd) }, leadingIcon = { Icon(Icons.Outlined.Edit, null, tint = C.Muted) },
                            onClick = { menuOpen = false; renaming = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Excluir playlist", style = T.bodyMd, color = C.Error) }, leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = C.Error) },
                            onClick = { menuOpen = false; deleting = true },
                        )
                    }
                }
            }
        }
        if (available.isNotEmpty()) {
            PlayAllButton("Reproduzir", { vm.startQueue(available, 0, name) }, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
        }
        if (keys.isEmpty()) {
            Empty(
                if (isFav) "Toque na estrela do player, ou no menu ⋮ de um vídeo, para favoritar." else "Use o menu ⋮ de um vídeo na biblioteca para adicioná-lo a esta playlist.",
                icon = if (isFav) Icons.Filled.Star else Icons.AutoMirrored.Outlined.PlaylistPlay, title = "Nada por aqui ainda",
            )
        } else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
            itemsIndexed(resolved, key = { _, p -> p.first }) { i, (key, video) ->
                if (video == null) {
                    MissingRow(key, onRemove = { if (isFav) UserStore.toggleFavorite(key) else UserStore.removeFromPlaylist(id, key) })
                } else {
                    val extras = buildList {
                        if (!isFav) {
                            if (i > 0) add(MenuAction("Subir", Icons.Outlined.ArrowUpward) { UserStore.movePlaylistItem(id, i, i - 1) })
                            if (i < resolved.lastIndex) add(MenuAction("Descer", Icons.Outlined.ArrowDownward) { UserStore.movePlaylistItem(id, i, i + 1) })
                            add(MenuAction("Tirar da playlist", Icons.Outlined.Delete) { UserStore.removeFromPlaylist(id, key) })
                        }
                    }
                    // Playlists e favoritos são filas próprias: o próximo é o próximo da lista
                    VideoRow(
                        video, video.folderLabel + " · " + videoDetails(video), null,
                        onClick = { val idx = available.indexOfFirst { it.key == key }; vm.startQueue(available, idx.coerceAtLeast(0), name) },
                        onAddToPlaylist = { addKey = key }, extraMenu = extras,
                    )
                }
            }
        }
    }

    if (renaming && playlist != null) PromptDialog("Renomear playlist", playlist.name, "Salvar", { UserStore.renamePlaylist(id, it); renaming = false }, { renaming = false })
    if (deleting && playlist != null) {
        AlertDialog(
            onDismissRequest = { deleting = false }, containerColor = C.CardHigh,
            title = { Text("Excluir “${playlist.name}”?", style = T.headlineSm) },
            text = { Text("Os vídeos continuam no aparelho; só a playlist é apagada.", style = T.bodyMd, color = C.Muted) },
            confirmButton = { TextButton({ UserStore.deletePlaylist(id); deleting = false; vm.openPlaylistId = null }) { Text("Excluir", style = T.labelLg, color = C.Error) } },
            dismissButton = { TextButton({ deleting = false }) { Text("Cancelar", style = T.labelLg, color = C.Muted) } },
        )
    }
    addKey?.let { k -> AddToPlaylistDialog(k, { addKey = null }, { vm.toast("Adicionado a “$it”") }) }
}

/** Vídeo de uma playlist que não está mais no aparelho (apagado ou movido). */
@Composable
private fun MissingRow(key: String, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(key.substringAfterLast('/').substringBeforeLast('.'), style = T.labelLg, color = C.Dim, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Arquivo não encontrado neste aparelho", style = T.labelSm, color = C.ErrorStrong)
        }
        IconButton(onRemove) { Icon(Icons.Outlined.Delete, "Tirar da lista", tint = C.Muted) }
    }
}
