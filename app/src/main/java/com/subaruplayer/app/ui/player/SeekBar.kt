package com.subaruplayer.app.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.subaruplayer.app.player.Chapter
import com.subaruplayer.app.ui.components.DarkBadge
import com.subaruplayer.app.ui.theme.C
import com.subaruplayer.app.util.formatTime
import kotlin.math.roundToInt

/**
 * Barra de progresso estilo YouTube, em dourado. Mostra os capítulos como pequenos cortes na trilha
 * e, ao arrastar, o tempo de destino numa bolha acima do dedo. O vídeo só pula quando o dedo solta.
 */
@Composable
fun SeekBar(
    positionMs: Long,
    durationMs: Long,
    chapters: List<Chapter>,
    onDragging: (Boolean) -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val duration = durationMs.coerceAtLeast(1)
    val fraction = dragFraction ?: (positionMs.toFloat() / duration).coerceIn(0f, 1f)
    val latestSeek by rememberUpdatedState(onSeek)
    val latestDragging by rememberUpdatedState(onDragging)
    val density = LocalDensity.current
    var widthPx by remember { mutableStateOf(0) }

    Box(modifier.fillMaxWidth().height(40.dp)) {
        Canvas(
            Modifier.fillMaxWidth().height(40.dp).pointerInput(durationMs) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    latestDragging(true)
                    var f = (down.position.x / size.width).coerceIn(0f, 1f)
                    dragFraction = f
                    while (true) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull { it.id == down.id } ?: ev.changes.first()
                        if (!c.pressed) break
                        f = (c.position.x / size.width).coerceIn(0f, 1f)
                        dragFraction = f
                        c.consume()
                    }
                    dragFraction = null
                    latestDragging(false)
                    latestSeek((f * duration).toLong())
                }
            },
        ) {
            widthPx = size.width.toInt()
            val trackH = 4.dp.toPx()
            val cy = size.height / 2
            val top = cy - trackH / 2
            drawRoundRect(Color(0x4DFFFFFF), Offset(0f, top), Size(size.width, trackH), CornerRadius(trackH / 2))
            drawRoundRect(C.Gold, Offset(0f, top), Size(size.width * fraction, trackH), CornerRadius(trackH / 2))
            // Capítulos: um corte fino na trilha em cada começo de capítulo
            chapters.drop(1).forEach { ch ->
                val x = size.width * (ch.startMs.toFloat() / duration)
                drawRect(Color(0xFF0F131C), Offset(x - 1.dp.toPx(), top - 1.dp.toPx()), Size(2.dp.toPx(), trackH + 2.dp.toPx()))
            }
            val thumbR = (if (dragFraction != null) 9.dp else 6.dp).toPx()
            drawCircle(C.Gold, thumbR, Offset(size.width * fraction, cy))
        }
        val df = dragFraction
        if (df != null && widthPx > 0) {
            val bubbleX = with(density) { (widthPx * df).roundToInt() }
            DarkBadge(
                formatTime((df * duration).toLong()) + chapterAt(chapters, (df * duration).toLong()),
                Modifier.align(Alignment.TopStart).offset { IntOffset((bubbleX - 22.dp.roundToPx()).coerceIn(0, (widthPx - 70.dp.roundToPx()).coerceAtLeast(0)), -10.dp.roundToPx()) },
            )
        }
    }
}

/** " · Nome do capítulo" para a bolha da barra, quando o vídeo tem capítulos. */
private fun chapterAt(chapters: List<Chapter>, ms: Long): String {
    val c = chapters.lastOrNull { it.startMs <= ms } ?: return ""
    return if (c.name.isNotBlank()) " · ${c.name.take(28)}" else ""
}
