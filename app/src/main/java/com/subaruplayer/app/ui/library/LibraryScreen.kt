package com.subaruplayer.app.ui.library

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.subaruplayer.app.AppViewModel
import com.subaruplayer.app.data.MediaRepository
import com.subaruplayer.app.data.SettingsStore
import com.subaruplayer.app.data.SortBy
import com.subaruplayer.app.data.UserStore
import com.subaruplayer.app.data.Video
import com.subaruplayer.app.ui.components.AddToPlaylistDialog
import com.subaruplayer.app.ui.components.BtnGhost
import com.subaruplayer.app.ui.components.BtnPrimary
import com.subaruplayer.app.ui.components.Empty
import com.subaruplayer.app.ui.components.FolderRow
import com.subaruplayer.app.ui.components.HRow
import com.subaruplayer.app.ui.components.PlayAllButton
import com.subaruplayer.app.ui.components.VideoCard
import com.subaruplayer.app.ui.components.VideoRow
import com.subaruplayer.app.ui.components.textFieldColors
import com.subaruplayer.app.ui.components.videoDetails
import com.subaruplayer.app.ui.theme.C
import com.subaruplayer.app.ui.theme.T

@Composable
fun LibraryScreen(vm: AppViewModel) {
    if (!vm.hasPermission) {
        PermissionGate(vm)
        return
    }
    val settings by SettingsStore.state.collectAsState()
    val user by UserStore.state.collectAsState()
    // Quanto de cada vídeo já foi assistido (só os que pararam no meio)
    val progress = remember(user.history) {
        user.history.filter { it.durationMs > 0 && it.positionMs > 0 }.associate { it.key to it.positionMs.toFloat() / it.durationMs }
    }
    var addKey by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        if (vm.searchOpen) SearchPanel(vm, progress) { addKey = it.key }
        else FolderView(vm, progress, settings.gridView) { addKey = it.key }
    }
    addKey?.let { key -> AddToPlaylistDialog(key, onDismiss = { addKey = null }, onAdded = { vm.toast("Adicionado a “$it”") }) }
}

@Composable
private fun PermissionGate(vm: AppViewModel) {
    val ctx = LocalContext.current
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.onPermissionResult() }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Empty(
            "Para mostrar seus vídeos, com cada um na sua pasta, o Subaru Player precisa de acesso aos vídeos do aparelho. Ele só lê, nunca altera nem envia nada.",
            icon = Icons.Outlined.VideoLibrary, title = "Permita o acesso aos vídeos",
        )
        BtnPrimary("Permitir acesso", { ask.launch(MediaRepository.permission) }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        BtnGhost("Abrir ajustes do app", {
            ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }, Modifier.fillMaxWidth())
    }
}

// ---------- Pastas ----------

@Composable
private fun FolderView(vm: AppViewModel, progress: Map<String, Float>, grid: Boolean, onAddToPlaylist: (Video) -> Unit) {
    val settings by SettingsStore.state.collectAsState()
    val library = vm.library
    val node = library.folder(vm.folderPath) ?: library.root
    val videos = remember(node, library, settings.sortBy, settings.sortDesc) { vm.sortedVideos(node) }

    Column(Modifier.fillMaxSize()) {
        Breadcrumb(vm.folderPath) { vm.openFolder(it) }
        Toolbar(
            canPlay = videos.isNotEmpty(),
            onPlay = { vm.playFolder(node) },
            grid = grid,
        )
        if (vm.loading && !vm.loadedOnce) {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = C.Gold, trackColor = C.CardHighest)
        }
        if (vm.loadedOnce && library.videos.isEmpty()) {
            Empty("Não achei nenhum vídeo neste aparelho. Os vídeos baixados pelo Subaru DL aparecem aqui sozinhos.", icon = Icons.Outlined.VideoLibrary, title = "Nenhum vídeo ainda")
            return@Column
        }
        if (node.folders.isEmpty() && videos.isEmpty() && vm.loadedOnce) {
            Empty("Esta pasta está vazia.", icon = Icons.Outlined.VideoLibrary)
            return@Column
        }
        LazyVerticalGrid(
            columns = if (grid) GridCells.Adaptive(160.dp) else GridCells.Fixed(1),
            state = vm.gridState(vm.folderPath),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp, start = if (grid) 8.dp else 0.dp, end = if (grid) 8.dp else 0.dp),
        ) {
            items(node.folders, key = { "f:" + it.path }, span = { GridItemSpan(maxLineSpan) }) { f ->
                FolderRow(f, onClick = { vm.openFolder(f.path) })
            }
            items(videos, key = { "v:" + it.key }) { v ->
                val sub = videoDetails(v)
                if (grid) VideoCard(v, sub, progress[v.key], { vm.playInFolder(v) }, { onAddToPlaylist(v) })
                else VideoRow(v, sub, progress[v.key], { vm.playInFolder(v) }, { onAddToPlaylist(v) })
            }
        }
    }
}

/** Trilha de pastas: mostra o caminho real e deixa voltar para qualquer nível. */
@Composable
private fun Breadcrumb(path: String, onOpen: (String) -> Unit) {
    val parts = path.trim('/').split('/').filter { it.isNotEmpty() }
    HRow(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), gap = 2.dp) {
        val rootActive = parts.isEmpty()
        Text(
            "Início", style = T.labelLg.copy(fontWeight = if (rootActive) FontWeight.Bold else FontWeight.Medium),
            color = if (rootActive) C.Gold else C.Muted,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onOpen("") }.padding(horizontal = 6.dp, vertical = 4.dp),
        )
        var acc = ""
        parts.forEachIndexed { i, p ->
            acc += "$p/"
            val target = acc
            val last = i == parts.lastIndex
            Icon(Icons.Outlined.ChevronRight, null, Modifier.size(16.dp), tint = C.Dim)
            Text(
                p, style = T.labelLg.copy(fontWeight = if (last) FontWeight.Bold else FontWeight.Medium), color = if (last) C.Gold else C.Muted,
                maxLines = 1,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onOpen(target) }.padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun Toolbar(canPlay: Boolean, onPlay: () -> Unit, grid: Boolean) {
    val settings by SettingsStore.state.collectAsState()
    var sortOpen by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (canPlay) PlayAllButton("Tocar pasta", onPlay, Modifier.weight(1f)) else Spacer(Modifier.weight(1f))
        Box {
            IconButton({ sortOpen = true }) { Icon(Icons.AutoMirrored.Outlined.Sort, "Ordenar", tint = C.BluePale) }
            DropdownMenu(sortOpen, { sortOpen = false }, containerColor = C.CardHigh) {
                listOf(SortBy.NAME to "Nome (1, 2, 10…)", SortBy.DATE to "Data", SortBy.DURATION to "Duração", SortBy.SIZE to "Tamanho").forEach { (by, label) ->
                    DropdownMenuItem(
                        text = { Text(label, style = T.bodyMd, color = if (settings.sortBy == by) C.Gold else C.Text) },
                        trailingIcon = { if (settings.sortBy == by) Icon(Icons.Filled.Check, null, tint = C.Gold) },
                        onClick = { SettingsStore.update { it.copy(sortBy = by) }; sortOpen = false },
                    )
                }
                DropdownMenuItem(
                    text = { Text(if (settings.sortDesc) "Ordem: decrescente" else "Ordem: crescente", style = T.bodyMd) },
                    onClick = { SettingsStore.update { it.copy(sortDesc = !it.sortDesc) }; sortOpen = false },
                )
            }
        }
        IconButton({ SettingsStore.update { it.copy(gridView = !it.gridView) } }) {
            Icon(if (grid) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView, if (grid) "Ver em lista" else "Ver em grade", tint = C.BluePale)
        }
    }
}

// ---------- Busca ----------

@Composable
private fun SearchPanel(vm: AppViewModel, progress: Map<String, Float>, onAddToPlaylist: (Video) -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val results = remember(vm.searchQuery, vm.library) { vm.library.search(vm.searchQuery) }
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            vm.searchQuery, { vm.searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).focusRequester(focus),
            singleLine = true, colors = textFieldColors(), shape = RoundedCornerShape(14.dp), textStyle = T.bodyMd,
            placeholder = { Text("Buscar por nome ou pasta") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingIcon = {
                IconButton({ if (vm.searchQuery.isNotEmpty()) vm.searchQuery = "" else vm.searchOpen = false }) { Icon(Icons.Outlined.Close, "Limpar") }
            },
        )
        when {
            vm.searchQuery.isBlank() -> Empty("Digite parte do nome do vídeo ou da pasta.", icon = Icons.Outlined.Search)
            results.isEmpty() -> Empty("Nada encontrado para “${vm.searchQuery.trim()}”.", icon = Icons.Outlined.Search)
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                item {
                    Text(
                        if (results.size == 1) "1 resultado" else "${results.size} resultados", style = T.labelMd,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                items(results, key = { it.key }) { v ->
                    // Mostra a pasta de cada resultado: tocar abre o vídeo seguindo a fila daquela pasta
                    VideoRow(v, v.folderLabel, progress[v.key], { vm.playInFolder(v) }, { onAddToPlaylist(v) })
                }
            }
        }
    }
}
