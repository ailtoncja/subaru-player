package com.subaruplayer.app.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.ZoomOutMap
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.subaruplayer.app.AppViewModel
import com.subaruplayer.app.data.SettingsStore
import com.subaruplayer.app.data.UserStore
import com.subaruplayer.app.player.Track
import com.subaruplayer.app.ui.components.BtnGhost
import com.subaruplayer.app.ui.components.ChipSmall
import com.subaruplayer.app.ui.components.Divider
import com.subaruplayer.app.ui.components.IconTile
import com.subaruplayer.app.ui.components.Num
import com.subaruplayer.app.ui.components.SubtitleStyleControls
import com.subaruplayer.app.ui.components.SwitchSubaru
import com.subaruplayer.app.ui.theme.C
import com.subaruplayer.app.ui.theme.T
import com.subaruplayer.app.util.formatTime
import java.util.Locale

enum class SheetPage { MENU, SPEED, SUBS, AUDIO, CHAPTERS, BOOKMARKS, QUEUE }

private val speeds = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f)

fun speedLabel(v: Float): String =
    (if (v % 1f == 0f) v.toInt().toString() else String.format(Locale.forLanguageTag("pt-BR"), "%.2f", v).trimEnd('0').trimEnd(',')) + "x"

private fun delayLabel(ms: Long) = String.format(Locale.forLanguageTag("pt-BR"), "%+.1f s", ms / 1000.0)

/** O libVLC dá nomes como "Disable" e "Track 1 - [English]"; aqui ficam em português. */
private fun trackLabel(t: Track): String = when {
    t.id == -1 -> "Desativada"
    else -> t.name.replace("Track", "Faixa").replace("Disable", "Desativada")
}

/** Menu do player (a engrenagem): cada página é uma parte do mesmo painel que sobe de baixo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSheet(vm: AppViewModel, page: SheetPage, onPage: (SheetPage) -> Unit, onDismiss: () -> Unit, onPickSubtitle: () -> Unit) {
    val st by vm.engine.state.collectAsState()
    val user by UserStore.state.collectAsState()
    val settings by SettingsStore.state.collectAsState()
    val queue = vm.queue ?: return
    val key = queue.current.key
    val bookmarks = user.bookmarks[key].orEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = C.Container, contentColor = C.Text, scrimColor = Color(0x99000000),
    ) {
        val back: (() -> Unit)? = if (page == SheetPage.MENU || page == SheetPage.QUEUE) null else ({ onPage(SheetPage.MENU) })
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            when (page) {
                SheetPage.MENU -> {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        SheetTitle("Ajustes do player", null)
                        MenuRow(Icons.Outlined.Speed, "Velocidade", speedLabel(st.speed)) { onPage(SheetPage.SPEED) }
                        MenuRow(Icons.Outlined.Subtitles, "Legendas", st.subTracks.firstOrNull { it.id == st.subTrackId }?.let { trackLabel(it) } ?: "Desativadas") { onPage(SheetPage.SUBS) }
                        MenuRow(Icons.Outlined.Audiotrack, "Áudio", st.audioTracks.firstOrNull { it.id == st.audioTrackId }?.let { trackLabel(it) } ?: "Padrão") { onPage(SheetPage.AUDIO) }
                        if (st.chapters.isNotEmpty()) MenuRow(Icons.Outlined.FormatListNumbered, "Capítulos", "${st.chapters.size}") { onPage(SheetPage.CHAPTERS) }
                        MenuRow(Icons.Outlined.BookmarkBorder, "Marcadores", if (bookmarks.isEmpty()) "Nenhum" else "${bookmarks.size}") { onPage(SheetPage.BOOKMARKS) }
                        MenuRow(Icons.AutoMirrored.Outlined.PlaylistPlay, "Fila", "${queue.items.size} vídeos") { onPage(SheetPage.QUEUE) }
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.engine.setFill(!st.fill) }.padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconTile(Icons.Outlined.ZoomOutMap, size = 38.dp)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Preencher a tela", style = T.labelLg)
                                Text("Corta as bordas para o vídeo cobrir tudo (ou pince a tela)", style = T.labelSm)
                            }
                            SwitchSubaru(st.fill, { vm.engine.setFill(it) }, gold = true)
                        }
                        Spacer(Modifier.size(16.dp))
                    }
                }
                SheetPage.SPEED -> Column(Modifier.verticalScroll(rememberScrollState())) {
                    SheetTitle("Velocidade", back)
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        speeds.forEach { v -> ChipSmall(speedLabel(v), st.speed == v, { vm.engine.setSpeed(v) }) }
                    }
                    Text("Segure o dedo na tela para tocar em ${speedLabel(settings.holdSpeed)} só enquanto segura.", style = T.labelSm, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                    Spacer(Modifier.size(16.dp))
                }
                SheetPage.SUBS -> Column(Modifier.verticalScroll(rememberScrollState())) {
                    SheetTitle("Legendas", back)
                    if (st.subTracks.isEmpty()) {
                        Text("Este vídeo não tem legendas embutidas. Você pode carregar um arquivo.", style = T.bodySm, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                    }
                    st.subTracks.forEach { t -> RadioRow(trackLabel(t), t.id == st.subTrackId) { vm.engine.setSubTrack(t.id) } }
                    BtnGhost("Carregar arquivo de legenda", onPickSubtitle, Modifier.padding(horizontal = 20.dp, vertical = 8.dp).fillMaxWidth(), icon = Icons.Outlined.FolderOpen)
                    Divider(Modifier.padding(vertical = 8.dp))
                    Stepper("Atraso da legenda", delayLabel(st.subDelayMs), { vm.engine.setSubDelayMs(st.subDelayMs - 100) }, { vm.engine.setSubDelayMs(st.subDelayMs + 100) }, { vm.engine.setSubDelayMs(0) })
                    Divider(Modifier.padding(vertical = 8.dp))
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        SubtitleStyleControls(
                            settings.subSize, settings.subColor,
                            onSize = { v -> SettingsStore.update { it.copy(subSize = v) }; vm.engine.restyle(SettingsStore.value) },
                            onColor = { v -> SettingsStore.update { it.copy(subColor = v) }; vm.engine.restyle(SettingsStore.value) },
                        )
                    }
                    Spacer(Modifier.size(16.dp))
                }
                SheetPage.AUDIO -> Column(Modifier.verticalScroll(rememberScrollState())) {
                    SheetTitle("Faixa de áudio", back)
                    if (st.audioTracks.isEmpty()) Text("Nenhuma faixa de áudio encontrada ainda.", style = T.bodySm, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                    st.audioTracks.forEach { t -> RadioRow(trackLabel(t), t.id == st.audioTrackId) { vm.engine.setAudioTrack(t.id) } }
                    Divider(Modifier.padding(vertical = 8.dp))
                    Stepper("Atraso do áudio", delayLabel(st.audioDelayMs), { vm.engine.setAudioDelayMs(st.audioDelayMs - 50) }, { vm.engine.setAudioDelayMs(st.audioDelayMs + 50) }, { vm.engine.setAudioDelayMs(0) })
                    Text("Se a voz chega antes da boca, aumente; se chega depois, diminua.", style = T.labelSm, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                    Spacer(Modifier.size(16.dp))
                }
                SheetPage.CHAPTERS -> Column(Modifier.verticalScroll(rememberScrollState())) {
                    SheetTitle("Capítulos", back)
                    val currentIdx = st.chapters.indexOfLast { it.startMs <= st.positionMs }
                    st.chapters.forEachIndexed { i, ch ->
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.engine.seekTo(ch.startMs); onDismiss() }.padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Num(formatTime(ch.startMs), Modifier.width(64.dp), color = if (i == currentIdx) C.Gold else C.Muted)
                            Text(
                                ch.name.ifBlank { "Capítulo ${i + 1}" }, Modifier.weight(1f),
                                style = T.labelLg.copy(fontWeight = if (i == currentIdx) FontWeight.Bold else FontWeight.Medium),
                                color = if (i == currentIdx) C.Gold else C.Text, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(Modifier.size(16.dp))
                }
                SheetPage.BOOKMARKS -> Column(Modifier.verticalScroll(rememberScrollState())) {
                    SheetTitle("Marcadores", back)
                    BtnGhost(
                        "Marcar ${formatTime(st.positionMs)}", { UserStore.addBookmark(key, st.positionMs, "Marcador ${bookmarks.size + 1}") },
                        Modifier.padding(horizontal = 20.dp, vertical = 8.dp).fillMaxWidth(), icon = Icons.Outlined.Add,
                    )
                    if (bookmarks.isEmpty()) Text("Marque um ponto do vídeo para voltar a ele depois. Cada vídeo tem os seus marcadores.", style = T.bodySm, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                    bookmarks.forEach { b ->
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.engine.seekTo(b.timeMs); onDismiss() }.padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Num(formatTime(b.timeMs), Modifier.width(64.dp), color = C.Gold)
                            Text(b.label, Modifier.weight(1f), style = T.labelLg)
                            IconButton({ UserStore.removeBookmark(key, b.timeMs) }) { Icon(Icons.Outlined.Close, "Apagar marcador", tint = C.Dim) }
                        }
                    }
                    Spacer(Modifier.size(16.dp))
                }
                SheetPage.QUEUE -> {
                    SheetTitle("Fila de reprodução", null)
                    QueueList(vm, Modifier.heightIn(max = 460.dp))
                }
            }
        }
    }
}

@Composable
private fun SheetTitle(title: String, onBack: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(start = if (onBack != null) 8.dp else 20.dp, end = 20.dp, top = 0.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconButton(onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar", tint = C.Text) }
        Text(title, style = T.headlineSm)
    }
}

@Composable
private fun MenuRow(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon, size = 38.dp)
        Spacer(Modifier.width(14.dp))
        Text(title, Modifier.weight(1f), style = T.labelLg)
        Text(value, style = T.labelMd, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 160.dp))
        Icon(Icons.Outlined.ChevronRight, null, tint = C.Dim)
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected, onClick, colors = RadioButtonDefaults.colors(selectedColor = C.Gold, unselectedColor = C.Dim))
        Text(label, style = T.bodyMd, color = if (selected) C.Gold else C.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** − valor + e um botão para zerar. */
@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit, onReset: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = T.labelLg)
        IconButton(onMinus) { Icon(Icons.Outlined.Remove, "Diminuir", tint = C.BluePale) }
        Num(value, Modifier.width(64.dp), size = 14, color = C.Gold)
        IconButton(onPlus) { Icon(Icons.Outlined.Add, "Aumentar", tint = C.BluePale) }
        Text(
            "Zerar", style = T.labelMd.copy(fontWeight = FontWeight.SemiBold), color = C.Muted,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onReset).padding(horizontal = 8.dp, vertical = 6.dp),
        )
    }
}
