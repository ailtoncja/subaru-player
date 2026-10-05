package com.subaruplayer.app.ui.player

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

/** O que a camada de toque do player avisa. Tudo o que não está aqui (brilho, volume) não existe de propósito. */
class PlayerGestureCallbacks(
    /** Toque curto. A tela decide se é simples (mostrar controles) ou parte de um toque duplo. */
    val onTap: (position: Offset, uptimeMillis: Long, size: IntSize) -> Unit,
    /** Dedo apoiado por um tempo: true ao começar a segurar, false ao soltar. */
    val onHold: (Boolean) -> Unit,
    val onSwipeUp: () -> Unit,
    val onSwipeDown: () -> Unit,
    /** Pinça: true = afastando os dedos (zoom), false = aproximando. */
    val onPinch: (zoomIn: Boolean) -> Unit,
)

private const val TAPPED = 0
private const val HOLDING = 1
private const val DRAGGING = 2
private const val PINCHING = 3

private fun distance(pressed: List<PointerInputChange>): Float = (pressed[0].position - pressed[1].position).getDistance()

/**
 * Um único detector para todos os gestos do vídeo, para que um não atrapalhe o outro:
 *  - toque curto → [PlayerGestureCallbacks.onTap];
 *  - dedo parado por um tempo → segurando (velocidade turbo até soltar);
 *  - arrasto vertical → entrar/sair da tela cheia;
 *  - dois dedos → pinça (preencher/ajustar).
 * Arrasto horizontal é ignorado: não há brilho, volume nem busca por arrasto.
 */
@Composable
fun Modifier.playerGestures(callbacks: PlayerGestureCallbacks): Modifier {
    val cb by rememberUpdatedState(callbacks)
    return pointerInput(Unit) {
        val slop = viewConfiguration.touchSlop
        val longPressMs = viewConfiguration.longPressTimeoutMillis
        val swipeMin = 56.dp.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val start = down.position
            var tapTime = 0L
            var pinchStart = 0f
            var pinchLast = 0f

            // Fase 1: até o dedo soltar (toque), andar (arrasto), chegar o 2º dedo (pinça) ou o tempo acabar (segurar)
            var mode = withTimeoutOrNull(longPressMs) {
                while (true) {
                    val ev = awaitPointerEvent()
                    val pressed = ev.changes.filter { it.pressed }
                    if (pressed.size >= 2) {
                        pinchStart = distance(pressed)
                        pinchLast = pinchStart
                        ev.changes.forEach { it.consume() }
                        return@withTimeoutOrNull PINCHING
                    }
                    val c = ev.changes.firstOrNull { it.id == down.id } ?: ev.changes.first()
                    if (!c.pressed) {
                        tapTime = c.uptimeMillis
                        return@withTimeoutOrNull TAPPED
                    }
                    if ((c.position - start).getDistance() > slop) return@withTimeoutOrNull DRAGGING
                }
                @Suppress("UNREACHABLE_CODE")
                TAPPED
            } ?: HOLDING

            if (mode == TAPPED) {
                cb.onTap(start, tapTime, size)
                return@awaitEachGesture
            }
            if (mode == HOLDING) {
                cb.onHold(true)
                while (true) {
                    val ev = awaitPointerEvent()
                    ev.changes.forEach { it.consume() }
                    if (ev.changes.none { it.pressed }) break
                }
                cb.onHold(false)
                return@awaitEachGesture
            }

            // Fase 2: arrasto vertical ou pinça, até todos os dedos saírem
            var dx = 0f
            var dy = 0f
            while (true) {
                val ev = awaitPointerEvent()
                val pressed = ev.changes.filter { it.pressed }
                if (pressed.size >= 2) {
                    val d = distance(pressed)
                    if (mode != PINCHING) { mode = PINCHING; pinchStart = d }
                    pinchLast = d
                    ev.changes.forEach { it.consume() }
                    continue
                }
                if (mode == PINCHING) {
                    if (pressed.isEmpty()) {
                        val ratio = pinchLast / pinchStart.coerceAtLeast(1f)
                        if (ratio > 1.12f) cb.onPinch(true) else if (ratio < 0.88f) cb.onPinch(false)
                        break
                    }
                    ev.changes.forEach { it.consume() }
                    continue
                }
                val c = ev.changes.firstOrNull { it.id == down.id } ?: ev.changes.first()
                if (!c.pressed) {
                    if (abs(dy) > swipeMin && abs(dy) > abs(dx) * 1.5f) {
                        if (dy < 0) cb.onSwipeUp() else cb.onSwipeDown()
                    }
                    break
                }
                dx = c.position.x - start.x
                dy = c.position.y - start.y
                c.consume()
            }
        }
    }
}
