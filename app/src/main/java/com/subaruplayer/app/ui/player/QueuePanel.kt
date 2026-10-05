package com.subaruplayer.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.subaruplayer.app.AppViewModel
import com.subaruplayer.app.ui.components.BtnGhost
import com.subaruplayer.app.ui.components.Num
import com.subaruplayer.app.ui.components.PromptDialog
import com.subaruplayer.app.ui.components.VideoThumb
import com.subaruplayer.app.ui.theme.C
import com.subaruplayer.app.ui.theme.T

/** "A seguir": a fila de reprodução. Tocar em um item pula para ele; o X tira da fila (menos o que está tocando). */
@Composable
fun QueueList(vm: AppViewModel, modifier: Modifier = Modifier) {
    val queue = vm.queue ?: return
    val listState = rememberLazyListState()
    var saving by remember { mutableStateOf(false) }

    // Mantém o vídeo atual visível quando a fila avança
    LaunchedEffect(queue.index) { listState.animateScrollToItem((queue.index - 1).coerceAtLeast(0)) }

    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("A seguir", style = T.title)
                Text("${queue.label} · ${queue.index + 1} de ${queue.items.size}", style = T.labelMd, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            BtnGhost("Salvar", { saving = true }, icon = Icons.AutoMirrored.Outlined.PlaylistAdd, height = 38.dp)
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false), state = listState) {
            itemsIndexed(queue.items, key = { _, v -> v.key }) { i, v ->
                val current = i == queue.index
                Row(
                    Modifier.fillMaxWidth()
                        .background(if (current) androidx.compose.ui.graphics.Color(0x1AFACC15) else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { vm.jumpTo(i) }.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(26.dp), contentAlignment = Alignment.Center) {
                        if (current) Icon(Icons.Outlined.GraphicEq, "Tocando", Modifier.size(18.dp), tint = C.Gold)
                        else Num("${i + 1}", color = C.Dim, size = 12)
                    }
                    VideoThumb(v, Modifier.width(96.dp).aspectRatio(16f / 9f))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        v.title, Modifier.weight(1f), style = T.labelLg.copy(fontWeight = if (current) FontWeight.Bold else FontWeight.SemiBold),
                        color = if (current) C.Gold else C.Text, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    if (!current && queue.items.size > 1) {
                        IconButton({ vm.removeFromQueue(i) }, Modifier.size(40.dp)) { Icon(Icons.Outlined.Close, "Tirar da fila", Modifier.size(18.dp), tint = C.Dim) }
                    } else Spacer(Modifier.width(40.dp))
                }
            }
        }
    }
    if (saving) PromptDialog(
        "Salvar fila como playlist", queue.label, "Salvar",
        onConfirm = { vm.saveQueueAsPlaylist(it.trim()); saving = false }, onDismiss = { saving = false },
    )
}
