package com.subaruplayer.app.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.view.OrientationEventListener
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.subaruplayer.app.AppViewModel
import com.subaruplayer.app.MainActivity
import com.subaruplayer.app.data.SettingsStore
import com.subaruplayer.app.data.UserStore
import com.subaruplayer.app.ui.components.AddToPlaylistDialog
import com.subaruplayer.app.ui.components.BtnGhost
import com.subaruplayer.app.ui.components.Divider
import com.subaruplayer.app.ui.components.HRow
import com.subaruplayer.app.ui.theme.C
import com.subaruplayer.app.ui.theme.T
import com.subaruplayer.app.util.formatTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.videolan.libvlc.util.VLCVideoLayout

private data class Hint(val text: String, val id: Long)
private data class SeekFeedback(val side: Int, val seconds: Int, val id: Long)

private enum class OrientationLock { NONE, LANDSCAPE, PORTRAIT }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * O player. Em pé: o vídeo no topo (16:9) e embaixo o título, as ações e "A seguir". Deitado ou em PiP: só o vídeo.
 * O vídeo fica sempre no mesmo lugar da árvore de composição, então girar a tela não reinicia a reprodução.
 */
@Composable
fun PlayerScreen(vm: AppViewModel, inPip: Boolean) {
    val queue = vm.queue ?: return
    val engine = vm.engine
    val st by engine.state.collectAsState()
    val settings by SettingsStore.state.collectAsState()
    val user by UserStore.state.collectAsState()
    val ctx = LocalContext.current
    val activity = remember(ctx) { ctx.findActivity() }
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val immersive = inPip || landscape
    val fullscreen = landscape && !inPip

    var controlsVisible by remember { mutableStateOf(true) }
    var interaction by remember { mutableIntStateOf(0) }
    var scrubbing by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<SheetPage?>(null) }
    var addToPlaylist by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<Hint?>(null) }
    var feedback by remember { mutableStateOf<SeekFeedback?>(null) }
    var lock by remember { mutableStateOf(OrientationLock.NONE) }

    val favorite = queue.current.key in user.favorites

    fun showHint(text: String) { hint = Hint(text, System.nanoTime()) }
    fun touch() { interaction++ }

    // ---------- Tela cheia ----------
    fun enterFullscreen() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        lock = OrientationLock.LANDSCAPE
    }
    fun exitFullscreen() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        lock = OrientationLock.PORTRAIT
    }

    // Quando o aparelho é virado de verdade para a posição que pedimos, solta a trava: dali em diante vale o giro automático
    DisposableEffect(lock) {
        val listener = object : OrientationEventListener(ctx) {
            override fun onOrientationChanged(deg: Int) {
                if (deg == ORIENTATION_UNKNOWN) return
                val portrait = deg <= 30 || deg >= 330 || deg in 150..210
                val land = deg in 60..120 || deg in 240..300
                if ((lock == OrientationLock.PORTRAIT && portrait) || (lock == OrientationLock.LANDSCAPE && land)) {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    lock = OrientationLock.NONE
                }
            }
        }
        if (lock != OrientationLock.NONE && listener.canDetectOrientation()) listener.enable()
        onDispose { listener.disable() }
    }

    // Barras do sistema: somem em tela cheia e no PiP
    LaunchedEffect(immersive) {
        val w = activity?.window ?: return@LaunchedEffect
        val c = WindowCompat.getInsetsController(w, view)
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (immersive) c.hide(WindowInsetsCompat.Type.systemBars()) else c.show(WindowInsetsCompat.Type.systemBars())
    }
    DisposableEffect(Unit) {
        onDispose {
            val w = activity?.window
            if (w != null) WindowCompat.getInsetsController(w, view).show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    DisposableEffect(st.playing) {
        view.keepScreenOn = st.playing
        onDispose { view.keepScreenOn = false }
    }

    BackHandler(enabled = !inPip) {
        if (fullscreen) exitFullscreen() else vm.closePlayer()
    }

    // Os controles somem sozinhos enquanto o vídeo toca
    LaunchedEffect(controlsVisible, st.playing, scrubbing, sheet, interaction) {
        if (controlsVisible && st.playing && !scrubbing && sheet == null) {
            delay(3500)
            controlsVisible = false
        }
    }
    LaunchedEffect(hint) { if (hint != null) { delay(1400); hint = null } }

    // ---------- Toque duplo que soma ----------
    var lastTapUptime by remember { mutableLongStateOf(0L) }
    var comboSide by remember { mutableIntStateOf(0) }
    var comboSeconds by remember { mutableIntStateOf(0) }
    var comboJob by remember { mutableStateOf<Job?>(null) }
    var singleJob by remember { mutableStateOf<Job?>(null) }

    fun seekSide(side: Int) {
        val skip = settings.skipSeconds
        comboSeconds = if (comboSide == side && comboJob?.isActive == true) comboSeconds + skip else skip
        comboSide = side
        engine.seekBy(side * skip * 1000L)
        feedback = SeekFeedback(side, comboSeconds, System.nanoTime())
        controlsVisible = false
        comboJob?.cancel()
        comboJob = scope.launch { delay(700); feedback = null; comboSide = 0 }
    }

    val callbacks = PlayerGestureCallbacks(
        onTap = { pos: Offset, uptime: Long, size: IntSize ->
            val side = if (pos.x < size.width / 2f) -1 else 1
            when {
                // Enquanto a "sequência" está ativa, cada toque simples soma mais um pulo
                comboJob?.isActive == true -> { singleJob?.cancel(); lastTapUptime = 0; seekSide(side) }
                lastTapUptime != 0L && uptime - lastTapUptime <= 300 -> { singleJob?.cancel(); lastTapUptime = 0; seekSide(side) }
                else -> {
                    lastTapUptime = uptime
                    singleJob?.cancel()
                    singleJob = scope.launch { delay(280); controlsVisible = !controlsVisible; touch() }
                }
            }
        },
        onHold = { on -> engine.boost(on, settings.holdSpeed) },
        onSwipeUp = { if (!immersive) enterFullscreen() },
        onSwipeDown = { if (immersive && !inPip) exitFullscreen() },
        onPinch = { zoomIn ->
            engine.setFill(zoomIn)
            showHint(if (zoomIn) "Preencher a tela" else "Ajustar à tela")
        },
    )

    val pickSubtitle = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) vm.addSubtitleFile(uri) }

    Column(
        Modifier.fillMaxSize().background(Color.Black).then(if (!immersive) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier),
    ) {
        val videoModifier = if (immersive) Modifier.weight(1f).fillMaxWidth() else Modifier.fillMaxWidth().aspectRatio(16f / 9f)
        Box(videoModifier.background(Color.Black)) {
            AndroidView(
                factory = { c -> VLCVideoLayout(c).also { engine.attach(it) } },
                onRelease = { engine.detach() },
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().playerGestures(callbacks))

            if (!inPip) {
                SeekFeedbackOverlay(feedback)
                if (st.boosting) Pill("${speedLabel(settings.holdSpeed)}  ▶▶", Modifier.align(Alignment.TopCenter).padding(top = 14.dp), C.Gold)
                hint?.let { h -> Pill(h.text, Modifier.align(Alignment.TopCenter).padding(top = 14.dp)) }
            }
            if ((!st.started || st.buffering) && !st.error) CircularProgressIndicator(Modifier.align(Alignment.Center).size(44.dp), color = C.Gold, trackColor = Color(0x33FFFFFF))
            if (st.error) ErrorPanel(
                onRetry = { vm.retryCurrent(false) }, onSoftware = { vm.retryCurrent(true) }, onNext = if (queue.hasNext) ({ vm.next() }) else null,
                modifier = Modifier.align(Alignment.Center),
            )

            if (!inPip) {
                val controlsAlpha by animateFloatAsState(if (controlsVisible && !st.error) 1f else 0f, tween(if (controlsVisible) 150 else 250), label = "controls")
                // Fora da composição quando invisível: assim os toques chegam à camada de gestos do vídeo
                if (controlsAlpha > 0.01f) Box(Modifier.fillMaxSize().alpha(controlsAlpha)) {
                    PlayerControls(
                        st, queue, favorite, fullscreen,
                        ControlActions(
                            onBack = { touch(); if (fullscreen) exitFullscreen() else vm.closePlayer() },
                            onFavorite = { touch(); UserStore.toggleFavorite(queue.current.key); showHint(if (favorite) "Tirado dos favoritos" else "Favoritado") },
                            onPip = if (Build.VERSION.SDK_INT >= 26 && settings.pip) ({ (activity as? MainActivity)?.enterPip() }) else null,
                            onQueue = { touch(); sheet = SheetPage.QUEUE },
                            onPrev = { touch(); vm.previous() },
                            onPlayPause = { touch(); engine.togglePlay() },
                            onNext = { touch(); vm.next() },
                            onScrubbing = { scrubbing = it },
                            onSeek = { touch(); engine.seekTo(it) },
                            onSettings = { touch(); sheet = SheetPage.MENU },
                            onFullscreen = { touch(); if (fullscreen) exitFullscreen() else enterFullscreen() },
                        ),
                    )
                }
                // Com os controles escondidos, sobra uma linha fina de progresso (como no YouTube)
                if (!controlsVisible && st.durationMs > 0) {
                    Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(2.dp).background(Color(0x33FFFFFF))) {
                        Box(Modifier.fillMaxWidth((st.positionMs.toFloat() / st.durationMs).coerceIn(0f, 1f)).height(2.dp).background(C.Gold))
                    }
                }
            }
        }

        if (!immersive) {
            Column(Modifier.weight(1f).fillMaxWidth().background(C.Surface)) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp)) {
                    Text(queue.current.title, style = T.headlineSm, maxLines = 3)
                    Text(queue.current.folderLabel, style = T.labelMd, modifier = Modifier.padding(top = 2.dp))
                    Spacer(Modifier.height(10.dp))
                    HRow {
                        BtnGhost(
                            if (favorite) "Favorito" else "Favoritar", { UserStore.toggleFavorite(queue.current.key) },
                            icon = if (favorite) Icons.Filled.Star else Icons.Outlined.StarBorder, color = if (favorite) C.Gold else C.Text, height = 40.dp,
                        )
                        BtnGhost("Playlist", { addToPlaylist = true }, icon = Icons.AutoMirrored.Outlined.PlaylistAdd, height = 40.dp)
                        BtnGhost("Marcar", {
                            val n = user.bookmarks[queue.current.key].orEmpty().size
                            UserStore.addBookmark(queue.current.key, st.positionMs, "Marcador ${n + 1}")
                            showHint("Marcado em ${formatTime(st.positionMs)}")
                        }, icon = Icons.Outlined.BookmarkBorder, height = 40.dp)
                        BtnGhost("Mais", { sheet = SheetPage.MENU }, icon = Icons.Outlined.MoreHoriz, height = 40.dp)
                    }
                }
                Divider()
                QueueList(vm, Modifier.weight(1f).navigationBarsPadding())
            }
        }
    }

    sheet?.let { page ->
        PlayerSheet(
            vm, page, onPage = { sheet = it }, onDismiss = { sheet = null; touch() },
            onPickSubtitle = { pickSubtitle.launch(arrayOf("*/*")) },
        )
    }
    if (addToPlaylist) AddToPlaylistDialog(queue.current.key, { addToPlaylist = false }, { vm.toast("Adicionado a “$it”") })
}

/** O brilho em meia-lua e o "10 segundos" do toque duplo, do lado tocado. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.SeekFeedbackOverlay(feedback: SeekFeedback?) {
    val last = remember { mutableStateOf<SeekFeedback?>(null) }
    SideEffect { if (feedback != null) last.value = feedback }
    val alpha by animateFloatAsState(if (feedback != null) 1f else 0f, tween(if (feedback != null) 80 else 220), label = "seek")
    val fb = feedback ?: last.value ?: return
    if (alpha <= 0.01f) return
    val left = fb.side < 0
    val shape = if (left) RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50) else RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50)
    Box(
        Modifier.align(if (left) Alignment.CenterStart else Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(0.4f).alpha(alpha).clip(shape).background(Color(0x40FFFFFF)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.material3.Icon(if (left) Icons.Outlined.FastRewind else Icons.Outlined.FastForward, null, Modifier.size(34.dp), tint = Color.White)
            Text("${fb.seconds} segundos", style = T.labelLg, color = Color.White, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun Pill(text: String, modifier: Modifier = Modifier, color: Color = Color.White) {
    Text(
        text, modifier.clip(RoundedCornerShape(50)).background(Color(0xB3000000)).padding(horizontal = 14.dp, vertical = 6.dp),
        style = T.labelLg.copy(fontWeight = FontWeight.Bold), color = color,
    )
}

@Composable
private fun ErrorPanel(onRetry: () -> Unit, onSoftware: () -> Unit, onNext: (() -> Unit)?, modifier: Modifier = Modifier) {
    Column(modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Não consegui reproduzir este vídeo", style = T.title, color = Color.White, textAlign = TextAlign.Center)
        HRow {
            BtnGhost("Tentar de novo", onRetry, height = 40.dp)
            BtnGhost("Por software", onSoftware, height = 40.dp)
            if (onNext != null) BtnGhost("Pular", onNext, height = 40.dp)
        }
    }
}
