package com.subaruplayer.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.subaruplayer.app.data.Queue
import com.subaruplayer.app.player.PlayerState
import com.subaruplayer.app.ui.components.Num
import com.subaruplayer.app.ui.theme.C
import com.subaruplayer.app.ui.theme.T
import com.subaruplayer.app.util.formatTime

class ControlActions(
    val onBack: () -> Unit,
    val onFavorite: () -> Unit,
    val onPip: (() -> Unit)?,
    val onQueue: () -> Unit,
    val onPrev: () -> Unit,
    val onPlayPause: () -> Unit,
    val onNext: () -> Unit,
    val onScrubbing: (Boolean) -> Unit,
    val onSeek: (Long) -> Unit,
    val onSettings: () -> Unit,
    val onFullscreen: () -> Unit,
)

/** Camada de controles sobre o vídeo: topo (voltar, título, favorito), centro (anterior, play/pause, próximo) e base (tempo e barra). */
@Composable
fun PlayerControls(
    st: PlayerState,
    queue: Queue,
    favorite: Boolean,
    fullscreen: Boolean,
    actions: ControlActions,
    modifier: Modifier = Modifier,
) {
    val safe = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
    Box(modifier.fillMaxSize()) {
        // Degradês para os ícones brancos terem leitura sobre qualquer cena
        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(96.dp).background(Brush.verticalGradient(listOf(Color(0xB3000000), Color.Transparent))))
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(120.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))))

        // Topo
        Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().then(safe).padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(actions.onBack) {
                Icon(if (fullscreen) Icons.AutoMirrored.Outlined.ArrowBack else Icons.Outlined.KeyboardArrowDown, if (fullscreen) "Sair da tela cheia" else "Fechar o player", tint = Color.White)
            }
            Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                Text(queue.current.title, style = T.labelLg, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${queue.label} · ${queue.index + 1}/${queue.items.size}", style = T.labelSm, color = Color(0xCCFFFFFF), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(actions.onFavorite) {
                Icon(if (favorite) Icons.Filled.Star else Icons.Outlined.StarBorder, if (favorite) "Tirar dos favoritos" else "Favoritar", tint = if (favorite) C.Gold else Color.White)
            }
            actions.onPip?.let { IconButton(it) { Icon(Icons.Outlined.PictureInPictureAlt, "Picture-in-picture", tint = Color.White) } }
            IconButton(actions.onQueue) { Icon(Icons.AutoMirrored.Outlined.PlaylistPlay, "Fila", tint = Color.White) }
        }

        // Centro
        Row(Modifier.align(Alignment.Center), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(Icons.Filled.SkipPrevious, "Anterior", 52.dp, 30.dp, actions.onPrev)
            val icon = when {
                st.ended -> Icons.Outlined.Replay
                st.playing -> Icons.Filled.Pause
                else -> Icons.Filled.PlayArrow
            }
            RoundIcon(icon, if (st.playing) "Pausar" else "Tocar", 68.dp, 42.dp, actions.onPlayPause)
            RoundIcon(Icons.Filled.SkipNext, "Próximo", 52.dp, 30.dp, actions.onNext, enabled = queue.hasNext)
        }

        // Base
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().then(safe).padding(horizontal = 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Num(formatTime(st.positionMs), color = Color.White)
                Num(" / ${formatTime(st.durationMs)}", color = Color(0xB3FFFFFF))
                Spacer(Modifier.weight(1f))
                IconButton(actions.onSettings, Modifier.size(40.dp)) { Icon(Icons.Outlined.Settings, "Ajustes do player", tint = Color.White) }
                IconButton(actions.onFullscreen, Modifier.size(40.dp)) {
                    Icon(if (fullscreen) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen, if (fullscreen) "Sair da tela cheia" else "Tela cheia", tint = Color.White)
                }
            }
            SeekBar(st.positionMs, st.durationMs, st.chapters, actions.onScrubbing, actions.onSeek)
        }
    }
}

@Composable
private fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, size: androidx.compose.ui.unit.Dp, iconSize: androidx.compose.ui.unit.Dp, onClick: () -> Unit, enabled: Boolean = true) {
    Box(
        Modifier.size(size).clip(CircleShape).background(Color(0x66000000)).alpha(if (enabled) 1f else 0.35f),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick, Modifier.size(size), enabled = enabled) { Icon(icon, description, Modifier.size(iconSize), tint = Color.White) }
    }
}
