package com.subaruplayer.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subaruplayer.app.AppViewModel
import com.subaruplayer.app.BuildConfig
import com.subaruplayer.app.data.HwMode
import com.subaruplayer.app.data.SettingsStore
import com.subaruplayer.app.ui.components.BtnGhost
import com.subaruplayer.app.ui.components.Divider
import com.subaruplayer.app.ui.components.IconTile
import com.subaruplayer.app.ui.components.Mono
import com.subaruplayer.app.ui.components.SCard
import com.subaruplayer.app.ui.components.SubtitleStyleControls
import com.subaruplayer.app.ui.components.Seg
import com.subaruplayer.app.ui.components.SectionLabel
import com.subaruplayer.app.ui.components.Segmented
import com.subaruplayer.app.ui.components.SwitchSubaru
import com.subaruplayer.app.ui.theme.C
import com.subaruplayer.app.ui.theme.T
import com.subaruplayer.app.util.BoxerSound

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val s by SettingsStore.state.collectAsState()
    val ctx = LocalContext.current
    fun set(block: (com.subaruplayer.app.data.AppSettings) -> com.subaruplayer.app.data.AppSettings) = SettingsStore.update(block)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Section("Reprodução") {
                RowItem(Icons.Outlined.Replay, "Retomar de onde parou", "Ao abrir um vídeo, continua do ponto em que você parou") {
                    SwitchSubaru(s.resume, { v -> set { it.copy(resume = v) } }, gold = true)
                }
                Divider()
                RowItem(Icons.AutoMirrored.Outlined.PlaylistPlay, "Tocar o próximo automaticamente", "Ao terminar, segue para o próximo vídeo da mesma pasta ou playlist") {
                    SwitchSubaru(s.autoNext, { v -> set { it.copy(autoNext = v) } }, gold = true)
                }
                Divider()
                RowItem(Icons.Outlined.PictureInPictureAlt, "Picture-in-picture", "Ao sair do app com um vídeo tocando, ele continua numa janelinha") {
                    SwitchSubaru(s.pip, { v -> set { it.copy(pip = v) } }, gold = true)
                }
                Divider()
                Block(Icons.Outlined.Replay, "Pulo do toque duplo", "Quantos segundos voltar ou avançar a cada toque duplo na tela") {
                    Segmented(listOf(5, 10, 15, 30).map { Seg(it, "$it s") }, s.skipSeconds, { v -> set { it.copy(skipSeconds = v) } })
                }
                Divider()
                Block(Icons.Outlined.Speed, "Velocidade ao segurar a tela", "Enquanto o dedo está apoiado, o vídeo toca nesta velocidade") {
                    Segmented(listOf(1.5f to "1,5x", 2f to "2x", 3f to "3x").map { Seg(it.first, it.second) }, s.holdSpeed, { v -> set { it.copy(holdSpeed = v) } })
                }
                Divider()
                Block(Icons.Outlined.Memory, "Decodificação", "Automática escolhe sozinha. Use Software se um vídeo travar ou ficar sem imagem") {
                    Segmented(
                        listOf(Seg(HwMode.AUTO, "Automática"), Seg(HwMode.HARDWARE, "Hardware"), Seg(HwMode.SOFTWARE, "Software")),
                        s.hwMode, { v -> set { it.copy(hwMode = v) } },
                    )
                }
            }
        }
        item {
            Section("Legendas") {
                Block(Icons.Outlined.Subtitles, "Estilo da legenda", "Tamanho e cor das legendas. Dentro do player a mudança vale na hora") {
                    SubtitleStyleControls(
                        s.subSize, s.subColor,
                        onSize = { v -> set { it.copy(subSize = v) }; vm.engine.restyle(SettingsStore.value) },
                        onColor = { v -> set { it.copy(subColor = v) }; vm.engine.restyle(SettingsStore.value) },
                    )
                }
            }
        }
        item {
            Section("Abertura") {
                RowItem(
                    Icons.AutoMirrored.Outlined.VolumeUp, "Som do Boxer ao abrir", "O ronco do motor toca quando o app abre (toque aqui para ouvir)",
                    onClick = { BoxerSound.play(ctx) },
                ) {
                    SwitchSubaru(s.boxerOnOpen, { v -> set { it.copy(boxerOnOpen = v) }; if (v) BoxerSound.play(ctx) }, gold = true)
                }
            }
        }
        item {
            Section("Biblioteca") {
                val folders = vm.library.root.let { countFolders(it) }
                RowItem(Icons.Outlined.Movie, "${vm.library.videos.size} vídeos em $folders pastas", "A biblioteca se atualiza sozinha quando chegam vídeos novos") {
                    BtnGhost("Atualizar", { vm.refresh(); vm.toast("Biblioteca atualizada") }, icon = Icons.Outlined.Refresh, height = 40.dp)
                }
            }
        }
        item {
            SCard(accent = C.Blue) {
                Mono("SUBARU PLAYER ${BuildConfig.VERSION_NAME} • BOXER MOTORSPORT", color = C.Gold, style = T.labelSm)
                Spacer(Modifier.height(6.dp))
                Text("Player de vídeos com o visual do Subaru DL. Motor de reprodução: VLC (libVLC ${BuildConfig.LIBVLC_VERSION}), da VideoLAN.", style = T.bodySm)
            }
        }
    }
}

private fun countFolders(n: com.subaruplayer.app.data.FolderNode): Int = n.folders.sumOf { 1 + countFolders(it) }

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(title, Modifier.padding(start = 4.dp))
        SCard(padding = 0.dp) { Column { content() } }
    }
}

@Composable
private fun RowItem(icon: ImageVector, title: String, subtitle: String?, onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon, size = 40.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = T.labelLg.copy(fontWeight = FontWeight.SemiBold))
            if (subtitle != null) Text(subtitle, style = T.labelSm)
        }
        Spacer(Modifier.width(10.dp))
        trailing()
    }
}

/** Linha com título e, embaixo, um controle largo (segmentos, cores). */
@Composable
private fun Block(icon: ImageVector, title: String, subtitle: String?, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon, size = 40.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = T.labelLg.copy(fontWeight = FontWeight.SemiBold))
                if (subtitle != null) Text(subtitle, style = T.labelSm)
            }
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}
