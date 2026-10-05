package com.subaruplayer.app.data

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat

/** Lê os vídeos do aparelho pelo MediaStore, guardando a pasta (RELATIVE_PATH) de cada um. */
object MediaRepository {

    fun hasPermission(ctx: Context): Boolean {
        val perm = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_EXTERNAL_STORAGE
        return ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED
    }

    val permission: String
        get() = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_EXTERNAL_STORAGE

    fun load(ctx: Context): List<Video> {
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.RELATIVE_PATH,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
        )
        val out = ArrayList<Video>()
        runCatching {
            ctx.contentResolver.query(collection, projection, null, null, null)?.use { c ->
                val iId = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val iName = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val iPath = c.getColumnIndexOrThrow(MediaStore.Video.Media.RELATIVE_PATH)
                val iDur = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val iSize = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val iDate = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val iW = c.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val iH = c.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                while (c.moveToNext()) {
                    val id = c.getLong(iId)
                    val name = c.getString(iName) ?: continue
                    var rel = c.getString(iPath).orEmpty()
                    if (rel.isNotEmpty() && !rel.endsWith("/")) rel += "/"
                    out += Video(
                        id = id,
                        uri = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL, id).toString(),
                        name = name,
                        relPath = rel,
                        durationMs = c.getLong(iDur),
                        size = c.getLong(iSize),
                        dateAdded = c.getLong(iDate),
                        width = c.getInt(iW),
                        height = c.getInt(iH),
                    )
                }
            }
        }
        return out
    }

    /** Vídeo aberto "de fora" (gerenciador de arquivos, WhatsApp…): se estiver na biblioteca usamos o dado completo. */
    fun describeExternal(ctx: Context, uri: Uri): Video {
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: "Vídeo"
        var size = 0L
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            runCatching {
                ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        c.getString(0)?.let { name = it }
                        size = c.getLong(1)
                    }
                }
            }
        }
        return Video(id = -1, uri = uri.toString(), name = name, relPath = "", durationMs = 0, size = size, dateAdded = 0, width = 0, height = 0)
    }

    /** Avisa quando entram ou saem vídeos (por exemplo, um download do Subaru DL terminou). */
    fun observe(ctx: Context, onChange: () -> Unit): () -> Unit {
        val handler = Handler(Looper.getMainLooper())
        val run = Runnable { onChange() }
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                handler.removeCallbacks(run)
                handler.postDelayed(run, 800)
            }
        }
        ctx.contentResolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer)
        return { ctx.contentResolver.unregisterContentObserver(observer) }
    }
}
