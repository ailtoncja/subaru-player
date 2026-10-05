package com.subaruplayer.app.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.subaruplayer.app.data.AppSettings
import com.subaruplayer.app.data.HwMode
import com.subaruplayer.app.data.Video
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.libvlc.util.VLCVideoLayout
import kotlin.math.abs

data class Track(val id: Int, val name: String)
data class Chapter(val name: String, val startMs: Long, val durationMs: Long)

data class PlayerState(
    val playing: Boolean = false,
    val buffering: Boolean = false,
    /** O libVLC já começou a tocar este vídeo (antes disso só há "carregando"). */
    val started: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val speed: Float = 1f,
    val ended: Boolean = false,
    val error: Boolean = false,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val audioTracks: List<Track> = emptyList(),
    val subTracks: List<Track> = emptyList(),
    val audioTrackId: Int = -1,
    val subTrackId: Int = -1,
    val chapters: List<Chapter> = emptyList(),
    val audioDelayMs: Long = 0,
    val subDelayMs: Long = 0,
    /** Zoom para preencher a tela (corta as bordas) em vez de mostrar o vídeo inteiro. */
    val fill: Boolean = false,
    /** Enquanto o dedo segura a tela, a velocidade sobe. */
    val boosting: Boolean = false,
)

/**
 * Embrulha o libVLC: um vídeo por vez, com o estado exposto como fluxo para a interface (Compose).
 * Tudo roda na thread principal; só a liberação do libVLC acontece em segundo plano para não travar a tela.
 */
class PlayerEngine(private val appContext: Context) {
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    /** Chamado quando o vídeo chega ao fim (a fila decide o que tocar depois). */
    var onEnded: (() -> Unit)? = null

    private var libVlc: LibVLC? = null
    private var player: MediaPlayer? = null
    private var layout: VLCVideoLayout? = null
    private var pfd: ParcelFileDescriptor? = null

    private var current: Video? = null
    private var hw: HwMode = HwMode.AUTO
    private var style = StyleKey(16, 0xFFFFFF)
    private var pendingStartMs = 0L
    private var released = false
    private var rateBeforeBoost = 1f

    private data class StyleKey(val size: Int, val color: Int)

    // ---------- Foco de áudio e fones ----------

    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) pause()
    }
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { pause() }
    }
    private var noisyRegistered = false

    private fun requestFocus() {
        val req = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MOVIE).build())
            .setOnAudioFocusChangeListener(focusListener)
            .build().also { focusRequest = it }
        audioManager.requestAudioFocus(req)
        if (!noisyRegistered) {
            appContext.registerReceiver(noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
            noisyRegistered = true
        }
    }

    private fun abandonFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        if (noisyRegistered) {
            runCatching { appContext.unregisterReceiver(noisyReceiver) }
            noisyRegistered = false
        }
    }

    // ---------- Criação e ligação com a tela ----------

    private fun vlcArgs(): ArrayList<String> = arrayListOf(
        "--audio-time-stretch",                      // muda a velocidade sem mudar o tom da voz
        "--freetype-rel-fontsize=${style.size}",     // tamanho da legenda (menor número = letra maior)
        "--freetype-color=${style.color}",           // cor da legenda
        "--freetype-outline-thickness=4",
        "--no-sub-autodetect-file",                  // legendas só as embutidas ou as que você escolher
    )

    private fun createPlayer() {
        val lib = LibVLC(appContext, vlcArgs())
        val mp = MediaPlayer(lib)
        mp.setEventListener { ev -> onEvent(mp, ev) }
        libVlc = lib
        player = mp
        layout?.let { mp.attachViews(it, null, true, false) }
        applyScale(mp)
    }

    private fun destroyPlayer() {
        val mp = player
        val lib = libVlc
        player = null
        libVlc = null
        val fd = pfd
        pfd = null
        if (mp != null) {
            mp.setEventListener(null)
            runCatching { mp.stop() }
            runCatching { mp.detachViews() }
        }
        // release() espera o decodificador parar: fora da thread principal para não travar a interface
        Thread {
            runCatching { mp?.release() }
            runCatching { lib?.release() }
            runCatching { fd?.close() }
        }.start()
    }

    /** Liga o player à view de vídeo da tela. Pode ser chamado de novo se a view for recriada. */
    fun attach(view: VLCVideoLayout) {
        layout = view
        player?.let {
            runCatching { it.detachViews() }
            it.attachViews(view, null, true, false)
            applyScale(it)
        }
    }

    fun detach() {
        layout = null
        player?.let { runCatching { it.detachViews() } }
    }

    // ---------- Carregar e controlar ----------

    fun load(video: Video, startMs: Long, settings: AppSettings) {
        released = false
        current = video
        hw = settings.hwMode
        style = StyleKey(settings.subSize, settings.subColor)
        if (player == null) createPlayer()
        else if (style != lastAppliedStyle) {
            // O estilo da legenda só vale na criação do libVLC: recria antes de tocar
            destroyPlayer(); createPlayer()
        }
        lastAppliedStyle = style
        startMedia(video, startMs, autoplay = true)
    }

    private var lastAppliedStyle: StyleKey? = null

    private fun startMedia(video: Video, startMs: Long, autoplay: Boolean) {
        val mp = player ?: return
        val lib = libVlc ?: return
        pendingStartMs = startMs
        _state.update {
            PlayerState(speed = it.speed, fill = it.fill, audioDelayMs = 0, subDelayMs = 0, positionMs = startMs, durationMs = video.durationMs, videoWidth = video.width, videoHeight = video.height)
        }
        runCatching { pfd?.close() }
        pfd = null
        val media = runCatching {
            val uri = Uri.parse(video.uri)
            if (uri.scheme == "content") {
                val p = appContext.contentResolver.openFileDescriptor(uri, "r") ?: error("sem acesso ao arquivo")
                pfd = p
                Media(lib, p.fileDescriptor)
            } else Media(lib, uri)
        }.getOrElse {
            _state.update { s -> s.copy(error = true) }
            return
        }
        when (hw) {
            HwMode.AUTO -> media.setHWDecoderEnabled(true, false)
            HwMode.HARDWARE -> media.setHWDecoderEnabled(true, true)
            HwMode.SOFTWARE -> media.setHWDecoderEnabled(false, false)
        }
        if (startMs > 1000) media.addOption(":start-time=${startMs / 1000.0}")
        mp.setMedia(media)
        media.release()
        mp.setRate(_state.value.speed)
        if (autoplay) {
            requestFocus()
            mp.play()
        }
    }

    private fun onEvent(mp: MediaPlayer, ev: MediaPlayer.Event) {
        if (mp !== player) return
        when (ev.type) {
            MediaPlayer.Event.Playing -> {
                _state.update { it.copy(playing = true, buffering = false, started = true, ended = false, error = false) }
                if (pendingStartMs > 1000 && abs(mp.time - pendingStartMs) > 3000) mp.setTime(pendingStartMs)
                pendingStartMs = 0
                // Depois de recriar o player (estilo da legenda), volta ao estado em que estava
                pendingSubTrack?.let { id -> if (id >= 0) mp.setSpuTrack(id); pendingSubTrack = null }
                if (pausedAfterStart) { pausedAfterStart = false; mp.pause() }
                refreshTracks(mp)
            }
            MediaPlayer.Event.Paused -> _state.update { it.copy(playing = false) }
            MediaPlayer.Event.Stopped -> _state.update { it.copy(playing = false) }
            MediaPlayer.Event.EndReached -> {
                _state.update { it.copy(playing = false, ended = true, positionMs = it.durationMs) }
                onEnded?.invoke()
            }
            MediaPlayer.Event.EncounteredError -> _state.update { it.copy(playing = false, error = true, buffering = false) }
            MediaPlayer.Event.Buffering -> _state.update { it.copy(buffering = ev.buffering < 100f) }
            MediaPlayer.Event.TimeChanged -> _state.update { it.copy(positionMs = ev.timeChanged) }
            MediaPlayer.Event.LengthChanged -> _state.update { it.copy(durationMs = ev.lengthChanged) }
            MediaPlayer.Event.ESAdded, MediaPlayer.Event.ESDeleted, MediaPlayer.Event.ESSelected, MediaPlayer.Event.Vout -> refreshTracks(mp)
        }
    }

    private fun refreshTracks(mp: MediaPlayer) {
        val audio = mp.audioTracks?.map { Track(it.id, it.name) }.orEmpty()
        val subs = mp.spuTracks?.map { Track(it.id, it.name) }.orEmpty()
        val chapters = runCatching { mp.getChapters(-1)?.map { Chapter(it.name.orEmpty(), it.timeOffset, it.duration) } }.getOrNull().orEmpty()
        val vt = runCatching { mp.currentVideoTrack }.getOrNull()
        _state.update {
            it.copy(
                audioTracks = audio, subTracks = subs,
                audioTrackId = mp.audioTrack, subTrackId = mp.spuTrack,
                chapters = chapters,
                videoWidth = if (vt != null && vt.width > 0) vt.width else it.videoWidth,
                videoHeight = if (vt != null && vt.height > 0) vt.height else it.videoHeight,
            )
        }
    }

    fun play() {
        val mp = player ?: return
        requestFocus()
        if (_state.value.ended) { mp.stop(); _state.update { it.copy(ended = false, positionMs = 0) } }
        mp.play()
    }

    fun pause() {
        val mp = player ?: return
        if (mp.isPlaying) mp.pause()
    }

    fun togglePlay() { if (_state.value.playing) pause() else play() }

    fun seekTo(ms: Long) {
        val mp = player ?: return
        val target = ms.coerceIn(0, (_state.value.durationMs.takeIf { it > 0 } ?: Long.MAX_VALUE))
        mp.setTime(target)
        _state.update { it.copy(positionMs = target) }
    }

    fun seekBy(deltaMs: Long) = seekTo(_state.value.positionMs + deltaMs)

    fun setSpeed(speed: Float) {
        player?.setRate(speed)
        _state.update { it.copy(speed = speed) }
    }

    /** Segurar a tela: velocidade turbo só enquanto o dedo está apoiado. */
    fun boost(on: Boolean, speed: Float) {
        val mp = player ?: return
        if (on && !_state.value.boosting) {
            rateBeforeBoost = _state.value.speed
            mp.setRate(speed)
            _state.update { it.copy(boosting = true) }
        } else if (!on && _state.value.boosting) {
            mp.setRate(rateBeforeBoost)
            _state.update { it.copy(boosting = false) }
        }
    }

    fun setAudioTrack(id: Int) {
        player?.setAudioTrack(id)
        _state.update { it.copy(audioTrackId = id) }
    }

    fun setSubTrack(id: Int) {
        player?.setSpuTrack(id)
        _state.update { it.copy(subTrackId = id) }
    }

    fun setAudioDelayMs(ms: Long) {
        player?.setAudioDelay(ms * 1000)
        _state.update { it.copy(audioDelayMs = ms) }
    }

    fun setSubDelayMs(ms: Long) {
        player?.setSpuDelay(ms * 1000)
        _state.update { it.copy(subDelayMs = ms) }
    }

    /** Carrega um arquivo de legenda externo (precisa ser um arquivo comum, não um content://). */
    fun addSubtitle(uri: Uri) {
        player?.addSlave(IMedia.Slave.Type.Subtitle, uri, true)
    }

    fun setFill(fill: Boolean) {
        _state.update { it.copy(fill = fill) }
        player?.let { applyScale(it) }
    }

    private fun applyScale(mp: MediaPlayer) {
        mp.setVideoScale(if (_state.value.fill) MediaPlayer.ScaleType.SURFACE_FIT_SCREEN else MediaPlayer.ScaleType.SURFACE_BEST_FIT)
    }

    fun setChapter(index: Int) {
        player?.setChapter(index)
    }

    /**
     * Tamanho e cor da legenda só são lidos na criação do libVLC. Se mudaram, recria o player
     * e continua do mesmo ponto, tocando ou pausado como estava.
     */
    fun restyle(settings: AppSettings) {
        val novo = StyleKey(settings.subSize, settings.subColor)
        val video = current ?: return
        if (novo == style || player == null) { style = novo; return }
        style = novo
        lastAppliedStyle = novo
        val s = _state.value
        val wasPlaying = s.playing
        val subId = s.subTrackId
        destroyPlayer()
        createPlayer()
        startMedia(video, s.positionMs, autoplay = true)
        if (!wasPlaying) pausedAfterStart = true
        pendingSubTrack = subId
    }

    private var pausedAfterStart = false
    private var pendingSubTrack: Int? = null

    // ---------- Fim ----------

    /** Para tudo e libera o libVLC. A engine pode ser usada de novo com [load]. */
    fun stopAndRelease() {
        abandonFocus()
        released = true
        current = null
        destroyPlayer()
        _state.value = PlayerState()
    }
}
