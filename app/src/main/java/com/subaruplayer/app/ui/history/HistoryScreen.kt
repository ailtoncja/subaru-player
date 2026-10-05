package com.subaruplayer.app.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import com.subaruplayer.app.AppViewModel
import com.subaruplayer.app.data.HistoryEntry
import com.subaruplayer.app.data.UserStore
import com.subaruplayer.app.data.Video
import com.subaruplayer.app.ui.components.AddToPlaylistDialog
import com.subaruplayer.app.ui.components.BtnGhost
import com.subaruplayer.app.ui.components.Empty
import com.subaruplayer.app.ui.components.MenuAction
import com.subaruplayer.app.ui.components.VideoRow
import com.subaruplayer.app.ui.theme.C
import com.subaruplayer.app.ui.theme.T
import com.subaruplayer.app.util.formatTime
import com.subaruplayer.app.util.pathLabel

@Composable
fun HistoryScreen(vm: AppViewModel) {
    val user by UserStore.state.collectAsState()
    var confirmClear by remember { mutableStateOf(false) }
    var addKey by remember { mutableStateOf<String?>(null) }

    if (user.history.isEmpty()) {
        Empty("Os vídeos que você assistir aparecem aqui, com o ponto onde parou.", icon = Icons.Outlined.History, title = "Histórico vazio")
        return
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Assistidos recentemente", style = T.headlineSm, modifier = Modifier.weight(1f))
            BtnGhost("Limpar", { confirmClear = true }, icon = Icons.Outlined.DeleteSweep)
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(user.history, key = { it.key }) { e ->
                val video = vm.library.byKey[e.key]
                val where = pathLabel(e.relPath)
                val sub = if (e.positionMs > 0) "$where · Parou em ${formatTime(e.positionMs)}" else "$where · Assistido"
                val remove = MenuAction("Tirar do histórico", Icons.Outlined.RemoveCircleOutline) { UserStore.removeFromHistory(e.key) }
                if (video != null) {
                    VideoRow(
                        video, sub, if (e.durationMs > 0 && e.positionMs > 0) e.positionMs.toFloat() / e.durationMs else null,
                        onClick = { vm.playInFolder(video) }, onAddToPlaylist = { addKey = e.key }, extraMenu = listOf(remove),
                    )
                } else {
                    VideoRow(missing(e), "$where · arquivo não encontrado", null, onClick = {}, onAddToPlaylist = {}, extraMenu = listOf(remove), enabled = false)
                }
            }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false }, containerColor = C.CardHigh,
            title = { Text("Limpar o histórico?", style = T.headlineSm) },
            text = { Text("Isso também apaga o ponto onde cada vídeo parou. Os vídeos não são apagados.", style = T.bodyMd, color = C.Muted) },
            confirmButton = { TextButton({ UserStore.clearHistory(); confirmClear = false }) { Text("Limpar", style = T.labelLg, color = C.Error) } },
            dismissButton = { TextButton({ confirmClear = false }) { Text("Cancelar", style = T.labelLg, color = C.Muted) } },
        )
    }
    addKey?.let { k -> AddToPlaylistDialog(k, { addKey = null }, { vm.toast("Adicionado a “$it”") }) }
}

/** Entrada do histórico cujo arquivo não existe mais. */
private fun missing(e: HistoryEntry) = Video(
    id = -1, uri = "", name = e.title, relPath = e.relPath, durationMs = e.durationMs, size = 0, dateAdded = 0, width = 0, height = 0,
)
