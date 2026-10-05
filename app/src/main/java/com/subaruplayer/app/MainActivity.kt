package com.subaruplayer.app

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.subaruplayer.app.data.SettingsStore
import com.subaruplayer.app.ui.AppRoot
import com.subaruplayer.app.ui.theme.SubaruTheme
import com.subaruplayer.app.util.BoxerSound
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()
    private var inPip by mutableStateOf(false)

    /** Botões da janelinha do PiP (anterior, play/pause, próximo). */
    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PLAY_PAUSE -> vm.engine.togglePlay()
                ACTION_PREV -> vm.previous()
                ACTION_NEXT -> vm.next()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent { SubaruTheme { AppRoot(vm, inPip) } }

        if (Build.VERSION.SDK_INT >= 26) {
            ContextCompat.registerReceiver(
                this, pipReceiver,
                IntentFilter().apply { addAction(ACTION_PLAY_PAUSE); addAction(ACTION_PREV); addAction(ACTION_NEXT) },
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            // Mantém a janelinha do PiP em dia: proporção do vídeo, ícone de play/pause e entrada automática
            lifecycleScope.launch { vm.engine.state.map { Triple(it.playing, it.videoWidth, it.videoHeight) }.distinctUntilChanged().collect { updatePipParams() } }
            lifecycleScope.launch { snapshotFlow { vm.queue?.let { it.index to it.items.size } }.distinctUntilChanged().collect { updatePipParams() } }
        }

        val opensVideo = intent?.action == Intent.ACTION_VIEW
        if (savedInstanceState == null) {
            if (!opensVideo && !BoxerSound.playedThisLaunch && SettingsStore.value.boxerOnOpen) {
                BoxerSound.playedThisLaunch = true
                BoxerSound.play(this)
            }
            handle(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handle(intent)
    }

    /** "Abrir com Subaru Player" a partir de um gerenciador de arquivos ou de outro app. */
    private fun handle(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) intent.data?.let { vm.openExternal(it) }
    }

    override fun onStart() {
        super.onStart()
        // O usuário pode ter dado a permissão nos ajustes do Android enquanto o app estava fechado
        vm.onPermissionResult()
    }

    /** Fora da tela (e fora do PiP) o vídeo pausa; ao fechar a janelinha do PiP o Android também chama onStop. */
    override fun onStop() {
        super.onStop()
        vm.onBackgrounded()
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= 26) runCatching { unregisterReceiver(pipReceiver) }
        super.onDestroy()
    }

    // ---------- Picture-in-picture ----------

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Do Android 12 em diante a entrada é automática (setAutoEnterEnabled); antes, é por aqui
        if (Build.VERSION.SDK_INT in 26..30 && vm.queue != null && vm.engine.state.value.playing && SettingsStore.value.pip) enterPip()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPip = isInPictureInPictureMode
    }

    fun enterPip() {
        if (Build.VERSION.SDK_INT < 26 || vm.queue == null) return
        runCatching { enterPictureInPictureMode(buildPipParams()) }
    }

    private fun updatePipParams() {
        if (Build.VERSION.SDK_INT < 26) return
        runCatching { setPictureInPictureParams(buildPipParams()) }
    }

    private fun buildPipParams(): PictureInPictureParams {
        val s = vm.engine.state.value
        val q = vm.queue
        // O Android só aceita proporções entre 1:2,39 e 2,39:1
        var w = if (s.videoWidth > 0) s.videoWidth else 16
        var h = if (s.videoHeight > 0) s.videoHeight else 9
        if (w.toFloat() / h > 2.39f) { w = 239; h = 100 } else if (h.toFloat() / w > 2.39f) { w = 100; h = 239 }
        val actions = listOf(
            pipAction(android.R.drawable.ic_media_previous, "Anterior", ACTION_PREV, 1),
            if (s.playing) pipAction(android.R.drawable.ic_media_pause, "Pausar", ACTION_PLAY_PAUSE, 2)
            else pipAction(android.R.drawable.ic_media_play, "Tocar", ACTION_PLAY_PAUSE, 2),
            pipAction(android.R.drawable.ic_media_next, "Próximo", ACTION_NEXT, 3),
        )
        return PictureInPictureParams.Builder()
            .setAspectRatio(Rational(w, h))
            .setActions(actions)
            .apply {
                if (Build.VERSION.SDK_INT >= 31) setAutoEnterEnabled(q != null && s.playing && SettingsStore.value.pip)
            }
            .build()
    }

    private fun pipAction(icon: Int, title: String, action: String, code: Int): RemoteAction {
        val pi = PendingIntent.getBroadcast(this, code, Intent(action).setPackage(packageName), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return RemoteAction(Icon.createWithResource(this, icon), title, title, pi)
    }

    private companion object {
        const val ACTION_PLAY_PAUSE = "com.subaruplayer.app.PIP_PLAY_PAUSE"
        const val ACTION_PREV = "com.subaruplayer.app.PIP_PREV"
        const val ACTION_NEXT = "com.subaruplayer.app.PIP_NEXT"
    }
}
