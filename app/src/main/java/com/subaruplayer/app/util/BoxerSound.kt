package com.subaruplayer.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.subaruplayer.app.R

/** O ronco do Boxer (o mesmo áudio do Subaru DL), tocado uma vez; segue o volume do sistema/notificações. */
object BoxerSound {
    /** Só toca uma vez por abertura do app (girar a tela ou voltar do PiP não repete). */
    var playedThisLaunch = false

    fun play(ctx: Context) {
        runCatching {
            val player = MediaPlayer()
            player.setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
            )
            ctx.applicationContext.resources.openRawResourceFd(R.raw.boxer_wrc).use { player.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            player.setOnCompletionListener { it.release() }
            player.setOnErrorListener { mp, _, _ -> mp.release(); true }
            player.prepare()
            player.start()
        }
    }
}
