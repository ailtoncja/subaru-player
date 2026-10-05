package com.subaruplayer.app.ui.components

import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.subaruplayer.app.data.FolderNode
import com.subaruplayer.app.data.UserStore
import com.subaruplayer.app.data.Video
import com.subaruplayer.app.ui.theme.C
import com.subaruplayer.app.ui.theme.T
import com.subaruplayer.app.util.formatDate
import com.subaruplayer.app.util.formatSize
import com.subaruplayer.app.util.formatTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ---------- Miniaturas ----------

/** Cache das miniaturas (cerca de 24 MB) para a lista rolar sem recarregar. */
private object ThumbCache {
    val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
}

/** Miniatura do vídeo, com a duração no canto e uma barrinha dourada de quanto já foi assistido. */
@Composable
fun VideoThumb(video: Video, modifier: Modifier = Modifier, progress: Float? = null) {
    val ctx = LocalContext.current
    val bmp by produceState<Bitmap?>(ThumbCache.cache.get(video.uri), video.uri) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                runCatching { ctx.contentResolver.loadThumbnail(Uri.parse(video.uri), Size(480, 270), null) }.getOrNull()
            }?.also { ThumbCache.cache.put(video.uri, it) }
        }
    }
    Box(modifier.clip(RoundedCornerShape(10.dp)).background(C.CardHighest), contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) Image(b.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Outlined.Movie, null, Modifier.size(26.dp), tint = C.Dim)
        if (video.durationMs > 0) DarkBadge(formatTime(video.durationMs), Modifier.align(Alignment.BottomEnd).padding(5.dp))
        if (progress != null && progress > 0f) {
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color(0x66000000))) {
                Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(3.dp).background(C.Gold))
            }
        }
    }
}

// ---------- Vídeo na lista e na grade ----------

data class MenuAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
fun VideoMenu(video: Video, onAddToPlaylist: () -> Unit, extra: List<MenuAction> = emptyList(), standard: Boolean = true) {
    var open by remember { mutableStateOf(false) }
    val user by UserStore.state.collectAsState()
    val fav = video.key in user.favorites
    Box {
        IconButton({ open = true }, Modifier.size(40.dp)) { Icon(Icons.Outlined.MoreVert, "Mais opções", tint = C.Muted) }
        DropdownMenu(open, { open = false }, containerColor = C.CardHigh) {
            if (standard) {
                DropdownMenuItem(
                    text = { Text(if (fav) "Tirar dos favoritos" else "Favoritar", style = T.bodyMd) },
                    leadingIcon = { Icon(if (fav) Icons.Filled.Star else Icons.Outlined.StarBorder, null, tint = C.Gold) },
                    onClick = { open = false; UserStore.toggleFavorite(video.key) },
                )
                DropdownMenuItem(
                    text = { Text("Adicionar à playlist", style = T.bodyMd) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.PlaylistAdd, null, tint = C.BluePale) },
                    onClick = { open = false; onAddToPlaylist() },
                )
            }
            extra.forEach { a ->
                DropdownMenuItem(
                    text = { Text(a.label, style = T.bodyMd) },
                    leadingIcon = { Icon(a.icon, null, tint = C.Muted) },
                    onClick = { open = false; a.onClick() },
                )
            }
        }
    }
}

/** Linha de vídeo: miniatura, título, onde está (pasta) ou detalhes, estrela de favorito e menu. */
@Composable
fun VideoRow(
    video: Video,
    subtitle: String,
    progress: Float?,
    onClick: () -> Unit,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
    extraMenu: List<MenuAction> = emptyList(),
    enabled: Boolean = true,
) {
    val user by UserStore.state.collectAsState()
    val fav = video.key in user.favorites
    Row(
        modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VideoThumb(video, Modifier.width(128.dp).aspectRatio(16f / 9f), progress)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(video.title, style = T.labelLg, color = if (enabled) C.Text else C.Dim, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, style = T.labelSm, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (fav) Icon(Icons.Filled.Star, "Favorito", Modifier.size(16.dp), tint = C.Gold)
        if (enabled || extraMenu.isNotEmpty()) VideoMenu(video, onAddToPlaylist, extraMenu, standard = enabled) else Spacer(Modifier.width(40.dp))
    }
}

/** Cartão de vídeo para a grade. */
@Composable
fun VideoCard(
    video: Video,
    subtitle: String,
    progress: Float?,
    onClick: () -> Unit,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val user by UserStore.state.collectAsState()
    val fav = video.key in user.favorites
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(6.dp)) {
        VideoThumb(video, Modifier.fillMaxWidth().aspectRatio(16f / 9f), progress)
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(video.title, style = T.labelLg, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = T.labelSm, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (fav) Icon(Icons.Filled.Star, "Favorito", Modifier.padding(top = 2.dp).size(15.dp), tint = C.Gold)
            VideoMenu(video, onAddToPlaylist)
        }
    }
}

/** "128 MB · 05/10/2026" */
fun videoDetails(v: Video): String = listOf(formatSize(v.size), formatDate(v.dateAdded)).joinToString(" · ")

// ---------- Pastas ----------

@Composable
fun FolderRow(node: FolderNode, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val parts = buildList {
        add(if (node.total == 1) "1 vídeo" else "${node.total} vídeos")
        if (node.folders.isNotEmpty()) add(if (node.folders.size == 1) "1 subpasta" else "${node.folders.size} subpastas")
    }
    Row(
        modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(Icons.Outlined.Folder, tint = C.Gold, size = 44.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(node.name, style = T.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(parts.joinToString(" · "), style = T.labelMd)
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = C.Dim)
    }
}

// ---------- Diálogos ----------

/** Pergunta um nome (nova playlist, renomear). */
@Composable
fun PromptDialog(title: String, initial: String, confirmLabel: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.CardHigh,
        title = { Text(title, style = T.headlineSm) },
        text = {
            OutlinedTextField(
                text, { text = it }, singleLine = true, colors = textFieldColors(), shape = RoundedCornerShape(14.dp),
                textStyle = T.bodyMd, placeholder = { Text("Nome") },
            )
        },
        confirmButton = { TextButton({ onConfirm(text) }, enabled = text.isNotBlank()) { Text(confirmLabel, style = T.labelLg, color = C.Gold) } },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar", style = T.labelLg, color = C.Muted) } },
    )
}

/** Escolhe a playlist onde o vídeo entra, ou cria uma nova já com ele. */
@Composable
fun AddToPlaylistDialog(videoKey: String, onDismiss: () -> Unit, onAdded: (String) -> Unit) {
    val user by UserStore.state.collectAsState()
    var creating by remember { mutableStateOf(user.playlists.isEmpty()) }
    if (creating) {
        PromptDialog(
            "Nova playlist", "", "Criar",
            onConfirm = { name -> UserStore.createPlaylist(name, listOf(videoKey)); onAdded(name.trim()); onDismiss() },
            onDismiss = { if (user.playlists.isEmpty()) onDismiss() else creating = false },
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.CardHigh,
        title = { Text("Adicionar à playlist", style = T.headlineSm) },
        text = {
            LazyColumn(Modifier.heightIn(max = 320.dp)) {
                items(user.playlists, key = { it.id }) { p ->
                    val has = videoKey in p.keys
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(enabled = !has) {
                            UserStore.addToPlaylist(p.id, videoKey); onAdded(p.name); onDismiss()
                        }.padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = T.bodyMd.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(if (p.keys.size == 1) "1 vídeo" else "${p.keys.size} vídeos", style = T.labelSm)
                        }
                        if (has) Icon(Icons.Filled.Check, "Já está nesta playlist", tint = C.Success)
                    }
                }
            }
        },
        confirmButton = { TextButton({ creating = true }) { Text("Nova playlist", style = T.labelLg, color = C.Gold) } },
        dismissButton = { TextButton(onDismiss) { Text("Fechar", style = T.labelLg, color = C.Muted) } },
    )
}

/** Atalho para o botão de tocar usado nos cabeçalhos de pasta e de playlist. */
@Composable
fun PlayAllButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) =
    BtnPrimary(label, onClick, modifier, icon = Icons.Outlined.PlayArrow, enabled = enabled, height = 44.dp)
